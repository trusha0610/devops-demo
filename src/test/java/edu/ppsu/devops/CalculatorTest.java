package edu.ppsu.devops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class CalculatorTest {

    private final Calculator calc = new Calculator();

    @Test
    void addsTwoNumbers() {
        assertEquals(5, calc.add(2, 3));
    }

    @Test
    void subtractsTwoNumbers() {
        assertEquals(5, calc.subtract(3, 2));
    }

    @Test
    void multipliesTwoNumbers() {
        assertEquals(6, calc.multiply(2, 3));
    }

    @Test
    void dividesTwoNumbers() {
        assertEquals(2, calc.divide(6, 3));
    }

    @Test
    void divideByZeroThrows() {
        assertThrows(
            ArithmeticException.class,
            () -> calc.divide(1, 0)
        );
    }
}