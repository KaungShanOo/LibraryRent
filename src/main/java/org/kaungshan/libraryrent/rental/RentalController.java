package org.kaungshan.libraryrent.rental;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;
    private final BatchRentalService batchRentalService;

    @GetMapping
    public List<RentalResponse> list(@RequestParam(required = false) Long memberId) {
        return rentalService.findAll(memberId);
    }

    @GetMapping("/overdue")
    public List<RentalResponse> overdue() {
        return rentalService.findOverdue();
    }

    @GetMapping("/{id}")
    public RentalResponse get(@PathVariable Long id) {
        return rentalService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse rent(@Valid @RequestBody RentRequest request) {
        return rentalService.rent(request);
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    public List<RentalResponse> rentAll(@Valid @RequestBody BatchRentRequest request) {
        return batchRentalService.rentAll(request);
    }

    @PostMapping("/{id}/return")
    public RentalResponse returnBook(@PathVariable Long id) {
        return rentalService.returnBook(id);
    }
}
