package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
public class CreditCardController {

    @Autowired
    private CreditCardRepository creditCardRepository; 

    @Autowired
    private UserRepository userRepository;


    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isPresent()) {
            Optional<CreditCard> existingCard = creditCardRepository.findByNumber(payload.getCardNumber());
            if(existingCard.isPresent()) {
                return ResponseEntity.badRequest().build();
            }
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setOwner(user.get()); 
            creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId()); // Return the ID of the new credit card
        } else {
            return ResponseEntity.notFound().build(); // User not found
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        List<CreditCard> creditCards = creditCardRepository.findByOwner_Id(userId);
        if (creditCards.isEmpty()) {
            return ResponseEntity.notFound().build(); // Returns 404 Not Found
        }

        List<CreditCardView> cardViews = new ArrayList<>();
        for (CreditCard card : creditCards) {
            cardViews.add(new CreditCardView(card.getIssuanceBank(), card.getNumber()));
        }
        return ResponseEntity.ok(cardViews); // Returns 200 OK with the list of card views
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        Optional<CreditCard> creditCard = creditCardRepository.findByNumber(creditCardNumber); //find creditcard by creditCardNumber
        if (creditCard.isPresent()) {
            if (creditCard.get().getOwner() != null) {
                return ResponseEntity.ok(creditCard.get().getOwner().getId());
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<?> updateBalances(@RequestBody UpdateBalancePayload[] payloads) {
        for (UpdateBalancePayload payload : payloads) {
            Optional<CreditCard> card = creditCardRepository.findByNumber(payload.getCreditCardNumber());
            if (!card.isPresent()) {
                return ResponseEntity.badRequest().body("Credit card number not associated with a card.");
            }

            TreeMap<LocalDate, Double> balanceMap = new TreeMap<>();
            Map<LocalDate, BalanceHistory> historyMap = new HashMap<>();
            for (BalanceHistory history : card.get().getBalanceHistories()) {
                balanceMap.put(history.getDate(), history.getBalance());
                historyMap.put(history.getDate(), history);
            }

            LocalDate payloadDate = payload.getBalanceDate();
            double payloadAmount = payload.getBalanceAmount();

            BalanceHistory existingHistory = historyMap.get(payloadDate); // Check if there is an existing history on the payload date

            if (existingHistory != null) {
                existingHistory.setBalance(payloadAmount);
            } else {
                // No existing history, create a new one
                existingHistory = new BalanceHistory();
                existingHistory.setDate(payloadDate);
                existingHistory.setBalance(payloadAmount);
                existingHistory.setCreditCard( card.get());
                card.get().getBalanceHistories().add(existingHistory);
            }

            // Calculate the difference and propagate changes
            double currentBalance = balanceMap.getOrDefault(payloadDate, 0.0);
            double difference = payloadAmount - currentBalance;

            // Update the balance map for calculation
            balanceMap.put(payloadDate, payloadAmount);

            for (LocalDate date = payloadDate.plusDays(1); !date.isAfter(LocalDate.now()); date = date.plusDays(1)) {
                double updatedBalance = balanceMap.getOrDefault(date, 0.0) + difference;
                balanceMap.put(date, updatedBalance);
                BalanceHistory subsequentHistory = historyMap.get(date);
                if (subsequentHistory == null) {
                    subsequentHistory = new BalanceHistory();
                    subsequentHistory.setDate(date);
                    subsequentHistory.setBalance(updatedBalance);
                    subsequentHistory.setCreditCard( card.get());
                    card.get().getBalanceHistories().add(subsequentHistory);
                } else {
                    subsequentHistory.setBalance(updatedBalance);
                }
            }
            creditCardRepository.save( card.get()); // Save the card with all updates
        }
        return ResponseEntity.ok("Balances updated successfully.");
    }

    
}
