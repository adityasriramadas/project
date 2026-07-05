package com.app.inventory.service;

import com.app.inventory.constants.InventoryMessages;
import com.app.inventory.constants.InventoryConfigConstants;
import com.app.inventory.constants.ProductFields;
import com.app.inventory.dto.CacheNames;
import com.app.inventory.dto.ProductRequest;
import com.app.common.exception.BusinessConflictException;
import com.app.common.exception.BadRequestException;
import com.app.common.exception.CommonApiErrorCode;
import com.app.common.exception.ResourceNotFoundException;
import com.app.common.util.LoggerUtil;
import com.app.inventory.mapper.InventoryMapper;
import com.app.inventory.repository.entity.Product;
import com.app.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.data.jpa.domain.Specification;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor(configWatcherThreadFactory());

    private volatile int threshold;
    private final AtomicBoolean watcherStarted = new AtomicBoolean(false);
    private WatchService watchService;

    @Value(InventoryConfigConstants.THRESHOLD_VALUE)
    private int initialThreshold;

    private ThreadFactory configWatcherThreadFactory() {
        return Thread.ofPlatform().name(InventoryConfigConstants.CONFIG_WATCHER_THREAD_NAME, 0).daemon().factory();
    }

    @PostConstruct
    public void init() {
        this.threshold = initialThreshold;
        startConfigWatcher();
    }

    @PreDestroy
    public void shutdown() {
        watcherExecutor.shutdownNow();
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LoggerUtil.warn("Failed to stop config watcher: {}", e.getMessage());
            }
        }
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_ID, allEntries = true)
    public Product createOrUpdateProduct(ProductRequest request) {

        var existingProduct = productRepository.findById(request.getId());
        if (existingProduct.isPresent()) {
            var product = existingProduct.get();
            inventoryMapper.updateEntity(request, product);
            return productRepository.save(product);
        }

        var product = inventoryMapper.toEntity(request, threshold);

        return productRepository.save(product);
    }


    public Page<Product> listProducts(boolean lowStockOnly, Pageable pageable) {
        LoggerUtil.debug("Listing products with lowStockOnly: {}", lowStockOnly);
        if (lowStockOnly) {
            var lowStockSpec = (Specification<Product>) (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(ProductFields.QUANTITY), threshold);
            return productRepository.findAll(lowStockSpec, pageable);
        }
        return productRepository.findAll(pageable);
    }

    @Cacheable(cacheNames = CacheNames.PRODUCTS_BY_ID, key = "#p0")
    public Product getProductById(String id) {
        LoggerUtil.debug("Getting product with id: {} - checking cache first", id);
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.error("Product not found with id: {}", id);
                    return new ResourceNotFoundException(InventoryMessages.PRODUCT_NOT_FOUND_PREFIX + id);
                });
    }

    @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_ID, key = "#p0")
    @Transactional
    public Product reserveStock(String id, int qty) {
        LoggerUtil.info("Reserving stock for product: {}, quantity: {}", id, qty);
        validateQuantity(qty);
        var o = productRepository.findByIdForUpdate(id);
        if (o.isEmpty()) {
            LoggerUtil.error("Product not found for stock reservation: {}", id);
            throw new ResourceNotFoundException(InventoryMessages.PRODUCT_NOT_FOUND_PREFIX + id);
        }
        var p = o.get();
        if (!Boolean.TRUE.equals(p.getActive())) {
            LoggerUtil.warn("Inactive product cannot be reserved: {}", id);
            throw new BusinessConflictException(
                    CommonApiErrorCode.PRODUCT_NOT_FOUND,
                    InventoryMessages.PRODUCT_NOT_FOUND_PREFIX + id);
        }
        if (p.getQuantity() < qty) {
            LoggerUtil.warn("Insufficient inventory for product {}: available={}, requested={}", id, p.getQuantity(), qty);
            throw new BusinessConflictException(
                    CommonApiErrorCode.INSUFFICIENT_INVENTORY,
                    InventoryMessages.INSUFFICIENT_INVENTORY_PREFIX + id);
        }
        p.setQuantity(p.getQuantity() - qty);
        productRepository.save(p);
        if (p.getQuantity() <= threshold) {
            LoggerUtil.warn("Inventory low for {}: remaining={} threshold={}", p.getId(), p.getQuantity(), threshold);
        }
        LoggerUtil.info("Successfully reserved {} units of product {}", qty, id);
        return p;
    }

    @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_ID, key = "#p0")
    @Transactional
    public Product releaseStock(String id, int qty) {
        LoggerUtil.info("Releasing stock for product: {}, quantity: {}", id, qty);
        validateQuantity(qty);
        var o = productRepository.findByIdForUpdate(id);
        if (o.isEmpty()) {
            LoggerUtil.error("Product not found for stock release: {}", id);
            throw new ResourceNotFoundException(InventoryMessages.PRODUCT_NOT_FOUND_PREFIX + id);
        }
        var p = o.get();
        p.setQuantity(p.getQuantity() + qty);
        var saved = productRepository.save(p);
        LoggerUtil.info("Successfully released {} units of product {}", qty, id);
        return saved;
    }

    private void validateQuantity(int qty) {
        if (qty <= 0) {
            LoggerUtil.error("Invalid quantity provided: {}, must be positive", qty);
            throw new BadRequestException(InventoryMessages.QUANTITY_MUST_BE_POSITIVE);
        }
    }

    private void startConfigWatcher() {
        if (!watcherStarted.compareAndSet(false, true)) {
            return;
        }
        var cfg = Paths.get(InventoryConfigConstants.CONFIG_FILE_PATH);
        try {
            if (Files.exists(cfg)) {
                loadThreshold(cfg);
            }
                var dir = cfg.getParent();
            if (dir != null && Files.exists(dir)) {
                watchService = FileSystems.getDefault().newWatchService();
                dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                watcherExecutor.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            var key = watchService.take();
                            for (var ev : key.pollEvents()) {
                                var changed = (Path) ev.context();
                                if (changed.endsWith(cfg.getFileName())) {
                                    loadThreshold(cfg);
                                }
                            }
                            if (!key.reset()) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (ClosedWatchServiceException e) {
                            break;
                        }
                    }
                    return null;
                });
            }
        } catch (IOException e) {
            LoggerUtil.warn("Could not start config watcher: {}", e.getMessage());
        }
    }

    private void loadThreshold(Path cfg) {
        try {
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(cfg)) {
                properties.load(reader);
            }
            var value = properties.getProperty(InventoryConfigConstants.THRESHOLD_PROPERTY);
            if (value != null && !value.isBlank()) {
                var parsed = Integer.parseInt(value.trim());
                this.threshold = parsed;
                LoggerUtil.info("Updated {} to {}", InventoryConfigConstants.THRESHOLD_PROPERTY, parsed);
            }
        } catch (IOException e) {
            LoggerUtil.warn("Failed loading config: {}", e.getMessage());
        } catch (NumberFormatException e) {
            LoggerUtil.warn("Invalid {} value in {}: {}", InventoryConfigConstants.THRESHOLD_PROPERTY, cfg, e.getMessage());
        }
    }
}
