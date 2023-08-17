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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class CreditCardController {

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    @Autowired
    public CreditCardController(CreditCardRepository creditCardRepository, UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        Optional<User> userOptional = userRepository.findById(payload.getUserId());

        // Check if user exists
        if(userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if card number is being used
        if(creditCardRepository.findByCardNumber(payload.getCardNumber()) != null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userOptional.get();

        // Create Credit Card
        CreditCard creditCard = new CreditCard();
        creditCard.setNumber(payload.getCardNumber());
        creditCard.setUser(user);
        creditCard.setIssuanceBank(payload.getCardIssuanceBank());

        // Update today's balance as 0
        BalanceHistory firstItem = new BalanceHistory();
        Instant currentDate = Instant.now();
        firstItem.setDate(currentDate);
        firstItem.setBalance(0);
        creditCard.getBalanceHistories().add(firstItem);
        user.getCreditCards().add(creditCard);

        // Save credit card
        CreditCard savedCreditCard = creditCardRepository.save(creditCard);

        return ResponseEntity.ok(savedCreditCard.getId());

    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        Optional<User> userOptional = userRepository.findById(userId);

        // If user does not exist, return not found
        if(userOptional.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        List<CreditCardView> creditCards = userOptional.get().getCreditCards().stream().map(creditCard -> new CreditCardView(creditCard.getIssuanceBank(), creditCard.getNumber())).toList();
        return ResponseEntity.ok(creditCards);
    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        CreditCard creditCard = creditCardRepository.findByCardNumber(creditCardNumber);
        if(creditCard == null){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(creditCard.getUser().getId());
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> updateBalance(@RequestBody UpdateBalancePayload[] payload) {

        // Make sure all card numbers in transactions are valid. We either update all or none.
        for(UpdateBalancePayload transaction: payload){
            CreditCard creditCard = creditCardRepository.findByCardNumber(transaction.getCreditCardNumber());
            if (creditCard == null){
                return ResponseEntity.badRequest().body(1);
            }
        }
        for(UpdateBalancePayload transaction: payload){
            CreditCard creditCard = creditCardRepository.findByCardNumber(transaction.getCreditCardNumber());
            creditCard.addTransactionToBalanceHistory(transaction.getTransactionAmount(), transaction.getTransactionTime());
            creditCardRepository.save(creditCard);
        }
        return ResponseEntity.ok(0);
    }
    
}
