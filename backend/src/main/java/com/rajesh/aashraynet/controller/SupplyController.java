package com.rajesh.aashraynet.controller;

import com.rajesh.aashraynet.dto.request.CreateSupplyRequest;
import com.rajesh.aashraynet.dto.request.RejectRequest;
import com.rajesh.aashraynet.dto.request.StockAdjustmentRequest;
import com.rajesh.aashraynet.dto.request.SupplyItemRequest;
import com.rajesh.aashraynet.dto.response.MovementResponse;
import com.rajesh.aashraynet.dto.response.StockResponse;
import com.rajesh.aashraynet.dto.response.SupplyRequestResponse;
import com.rajesh.aashraynet.entity.RequestStatus;
import com.rajesh.aashraynet.entity.SupplyItem;
import com.rajesh.aashraynet.security.CurrentUser;
import com.rajesh.aashraynet.service.StockService;
import com.rajesh.aashraynet.service.SupplyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Supplies and stock")
public class SupplyController {
    private final SupplyService supplies;
    private final StockService stock;

    public SupplyController(SupplyService supplies, StockService stock) {
        this.supplies = supplies;
        this.stock = stock;
    }

    public record ItemResponse(Long id, String name, String unit, BigDecimal perPersonPerDay) {
        static ItemResponse from(SupplyItem i) { return new ItemResponse(i.getId(), i.getName(), i.getUnit(), i.getPerPersonPerDay()); }
    }

    @GetMapping("/supply-items")
    public List<ItemResponse> items() {
        return supplies.catalogue().stream().map(ItemResponse::from).toList();
    }

    @PostMapping("/supply-items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STATE_ADMIN')")
    public ItemResponse createItem(@Valid @RequestBody SupplyItemRequest req) {
        return ItemResponse.from(supplies.createItem(req, CurrentUser.get()));
    }

    @GetMapping("/shelters/{id}/stock")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public List<StockResponse> stock(@PathVariable Long id) {
        return stock.stockFor(id, CurrentUser.get());
    }

    @PostMapping("/shelters/{id}/stock/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN')")
    public MovementResponse adjust(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest req) {
        return stock.adjust(id, req, CurrentUser.get());
    }

    @GetMapping("/shelters/{id}/stock/movements")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public Page<MovementResponse> movements(@PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return stock.movements(id, Pages.capped(pageable), CurrentUser.get());
    }

    @PostMapping("/shelters/{id}/supply-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SHELTER_INCHARGE')")
    public SupplyRequestResponse create(@PathVariable Long id, @Valid @RequestBody CreateSupplyRequest req) {
        return supplies.create(id, req, CurrentUser.get());
    }

    @GetMapping("/supply-requests")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public Page<SupplyRequestResponse> list(@RequestParam(required = false) Long districtId,
                                            @RequestParam(name = "shelter", required = false) Long shelterId,
                                            @RequestParam(required = false) RequestStatus status,
                                            @PageableDefault(size = 20, sort = "createdAt",
                                                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return supplies.list(districtId, shelterId, status, Pages.capped(pageable), CurrentUser.get());
    }

    @GetMapping("/supply-requests/{id}")
    @PreAuthorize("hasAnyRole('SHELTER_INCHARGE','DISTRICT_ADMIN','STATE_ADMIN')")
    public SupplyRequestResponse get(@PathVariable Long id) {
        return supplies.get(id, CurrentUser.get());
    }

    @PostMapping("/supply-requests/{id}/approve")
    @PreAuthorize("hasRole('DISTRICT_ADMIN')")
    public SupplyRequestResponse approve(@PathVariable Long id) {
        return supplies.approve(id, CurrentUser.get());
    }

    @PostMapping("/supply-requests/{id}/reject")
    @PreAuthorize("hasRole('DISTRICT_ADMIN')")
    public SupplyRequestResponse reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
        return supplies.reject(id, req.reason(), CurrentUser.get());
    }

    @PostMapping("/supply-requests/{id}/dispatch")
    @PreAuthorize("hasRole('DISTRICT_ADMIN')")
    public SupplyRequestResponse dispatch(@PathVariable Long id) {
        return supplies.dispatch(id, CurrentUser.get());
    }

    @PostMapping("/supply-requests/{id}/receive")
    @PreAuthorize("hasRole('SHELTER_INCHARGE')")
    public SupplyRequestResponse receive(@PathVariable Long id) {
        return supplies.receive(id, CurrentUser.get());
    }
}
