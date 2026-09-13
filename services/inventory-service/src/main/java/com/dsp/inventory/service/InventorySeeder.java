package com.dsp.inventory.service;

import com.dsp.inventory.domain.InventoryItem;
import com.dsp.inventory.repo.InventoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Seeds demo stock on first startup only, so restarts don't reset stock levels mid-demo/load-test. */
@Component
public class InventorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);
    private static final int SEED_ITEM_COUNT = 5;
    private static final int SEED_AVAILABLE_QUANTITY = 1000;

    private final InventoryItemRepository repository;

    public InventorySeeder(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Inventory table already populated ({} items), skipping seed", repository.count());
            return;
        }
        List<InventoryItem> items = IntStream.rangeClosed(1, SEED_ITEM_COUNT)
                .mapToObj(i -> new InventoryItem("sku-" + i, SEED_AVAILABLE_QUANTITY, 0))
                .toList();
        repository.saveAll(items);
        log.info("Seeded {} demo inventory items with {} units each", SEED_ITEM_COUNT, SEED_AVAILABLE_QUANTITY);
    }
}
