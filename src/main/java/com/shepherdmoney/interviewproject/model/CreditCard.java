package com.shepherdmoney.interviewproject.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String issuanceBank;

    private String number;

    @ManyToOne
    private User user;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "credit_card_id")
    @OrderBy("date DESC")
    private List<BalanceHistory> balanceHistories = new ArrayList<>();

    public void addTransactionToBalanceHistory(double transactionAmount, Instant time){
        BalanceHistory balanceHistory = new BalanceHistory();
        balanceHistory.setDate(time);

        //Find position of balance history in balanceHistories
        int index = Collections.binarySearch(balanceHistories, balanceHistory, (o1, o2) -> {
            LocalDate date1 = o1.getDate().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate date2 = o2.getDate().atZone(ZoneId.systemDefault()).toLocalDate();
            return date2.compareTo(date1); // Reverse order to have most recent date first
        });

        // if index is within list range then the date already exists. Update all balances from this day to the present with the new amount.
        if (index >= 0 && index < balanceHistories.size()) {
            for (int i = index; i >= 0; i--) {
                BalanceHistory balanceHistoryTemp = balanceHistories.get(i);
                balanceHistoryTemp.setBalance(balanceHistoryTemp.getBalance() + transactionAmount);
                balanceHistories.set(i, balanceHistoryTemp);
            }
        } else {
            index = -(index + 1); // Convert negative insertion point to index

            // Add this balance to list
            balanceHistory.setBalance(transactionAmount);
            balanceHistories.add(balanceHistory);

            // Update all balances from this day to the present with the new amount.
            // Additionally, if there exists a gap in the days, fill those up.
            // For example if this is the current array: [
            //      {date: '2023-08-17', balance: 1200},
            //      {date: '2023-08-16', balance: 1000},
            //    ]
            // and you have to add this transaction, {date: '2023-08-13', balance: 150}, all dates from 08/13 to 08/15 have to added to
            // get the following:  [
            //      {date: '2023-08-17', balance: 1350},
            //      {date: '2023-08-16', balance: 1150},
            //      {date: '2023-08-15', balance: 150},
            //      {date: '2023-08-14', balance: 150},
            //      {date: '2023-08-13', balance: 150},
            //    ]

            index--;
            while(index >= 0) {
                BalanceHistory balanceHistoryTemp = balanceHistories.get(index);
                BalanceHistory prevDayBalanceHistoryTemp = balanceHistories.get(index + 1);

                // Calculate the difference in days between the dates
                long daysDifference = Duration.between(prevDayBalanceHistoryTemp.getDate(), balanceHistoryTemp.getDate()).toDays();

                if (daysDifference == 1) {
                    balanceHistoryTemp.setBalance(balanceHistoryTemp.getBalance() + transactionAmount);
                    balanceHistories.set(index, balanceHistoryTemp);
                    index--;
                } else {
                    // This condition is for scenarios discussed above regarding gaps.
                    // Get the date one day after prevDayBalanceHistoryTemp's date
                    Instant dateOneDayAfter = prevDayBalanceHistoryTemp.getDate().plus(Duration.ofDays(1));
                    BalanceHistory newDayBalanceHistory = new BalanceHistory();
                    newDayBalanceHistory.setDate(dateOneDayAfter);
                    newDayBalanceHistory.setBalance(prevDayBalanceHistoryTemp.getBalance());
                    balanceHistories.add(index + 1, newDayBalanceHistory);
                }
            }
        }
    }
}
