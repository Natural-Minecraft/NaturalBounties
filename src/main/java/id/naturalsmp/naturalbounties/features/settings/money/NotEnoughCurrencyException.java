package me.naturalsmp.NaturalBounties.features.settings.money;

public class NotEnoughCurrencyException extends Exception {
    public NotEnoughCurrencyException(String message) {
        super(message);
    }
}
