package com.dsp.inventory.api;

import com.dsp.inventory.repo.InventoryItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryItemRepository repository;

    public InventoryController(InventoryItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{itemId}")
    public InventoryResponse get(@PathVariable String itemId) {
        return repository.findById(itemId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No item with id " + itemId));
    }
}
