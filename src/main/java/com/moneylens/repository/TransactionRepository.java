package com.moneylens.repository;

import com.moneylens.model.Category;
import com.moneylens.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdAndCategory(
            Long userId, Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionDate >= :startDate " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findDebitsInRange(@Param("userId") Long userId,
                                        @Param("startDate") LocalDate startDate);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT")
    Double sumDebitsInRange(@Param("userId") Long userId,
                            @Param("start") LocalDate start,
                            @Param("end") LocalDate end);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "AND t.type = com.moneylens.model.TransactionType.CREDIT")
    Double sumCreditsInRange(@Param("userId") Long userId,
                             @Param("start") LocalDate start,
                             @Param("end") LocalDate end);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumByCategoryInRange(@Param("userId") Long userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Query("SELECT t.merchant, SUM(t.amount), COUNT(t) FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "GROUP BY t.merchant ORDER BY SUM(t.amount) DESC")
    List<Object[]> topMerchantsInRange(@Param("userId") Long userId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    @Query("SELECT FUNCTION('DATE', t.transactionDate), SUM(t.amount) " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT " +
           "GROUP BY FUNCTION('DATE', t.transactionDate) ORDER BY FUNCTION('DATE', t.transactionDate)")
    List<Object[]> dailySpendTrend(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("SELECT AVG(dailySum) FROM " +
           "(SELECT SUM(t.amount) as dailySum FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT " +
           "AND t.transactionDate >= :startDate " +
           "GROUP BY t.transactionDate) daily")
    Double avgDailySpend(@Param("userId") Long userId,
                         @Param("startDate") LocalDate startDate);

    @Query("SELECT t FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.transactionDate = :date " +
           "AND t.type = com.moneylens.model.TransactionType.DEBIT")
    List<Transaction> findByUserIdAndDate(@Param("userId") Long userId,
                                          @Param("date") LocalDate date);

    long countByUserIdAndIsRecurringTrue(Long userId);
}
