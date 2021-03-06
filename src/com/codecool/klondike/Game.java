package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;

import javax.swing.*;
import java.util.*;

import static com.codecool.klondike.Card.isOppositeColor;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Rank rank;
    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();
    private List<Pile> allPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();

        boolean forTableau = card.getContainingPile().getPileType() == Pile.PileType.TABLEAU && card.getContainingPile().getTopCard() == card &&
                e.getClickCount() == 2;

        boolean forDiscard = card.getContainingPile().getPileType() == Pile.PileType.DISCARD && card.getContainingPile().getTopCard() == card &&
                e.getClickCount() == 2;

        if(forTableau || forDiscard){
            Pile cardInitialPile = card.getContainingPile();

            Pile destPile = getDestPile(card,foundationPiles);
            card.moveToPile(destPile);

            if (cardInitialPile.numOfCards() > 1) {

                int cardIndex = -1;

                for (int i = 0; i < cardInitialPile.numOfCards(); i++) {
                    if (!cardInitialPile.getCards().get(i).equals(card)) cardIndex = i;
                }
                Card cardUnderDragged = (cardIndex -1  >= 0) ? cardInitialPile.getCards().get(cardIndex) : null;

                if (cardUnderDragged != null && cardUnderDragged.isFaceDown() && card.getContainingPile() != cardInitialPile){
                    cardUnderDragged.flip();
                }
            }

        }


        else if (card.getContainingPile().getPileType() == Pile.PileType.STOCK && card.getContainingPile().getTopCard() == card) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        } else if (card.getContainingPile().getPileType() == Pile.PileType.TABLEAU &&
                card.getContainingPile().getTopCard().isFaceDown()) {
            card.flip();
        }
    };


    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        draggedCards.clear();
        boolean draggable = false;
        Card clickedCard = (Card) e.getSource();
        Pile activePile = clickedCard.getContainingPile();
        boolean isStockUnderCard = activePile.getPileType().equals(Pile.PileType.DISCARD) && !clickedCard.equals(activePile.getTopCard());

        if ( isStockUnderCard || clickedCard.isFaceDown())

            return;

        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        for(Card card : activePile.getCards()){
            if(clickedCard == card || draggable){
                draggable = true;
                draggedCards.add(card);
            }
        }

        for(Card draggedCard : draggedCards){
            draggedCard.getDropShadow().setRadius(20);
            draggedCard.getDropShadow().setOffsetX(10);
            draggedCard.getDropShadow().setOffsetY(10);
            draggedCard.toFront();
            draggedCard.setTranslateX(offsetX);
            draggedCard.setTranslateY(offsetY);
        }
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, allPiles);
        //TODO
        if (pile != null) {
            Pile cardInitialPile = card.getContainingPile();
            if (cardInitialPile.numOfCards() > 1) {
                int cardIndex = -1;
                for (int i=0; i < cardInitialPile.numOfCards(); i++) {
                    if (cardInitialPile.getCards().get(i).equals(card)) cardIndex = i;
                }

                Card cardUnderDragged = (cardIndex-1 >= 0) ? cardInitialPile.getCards().get(cardIndex - 1) : null;

                if (cardUnderDragged != null && cardUnderDragged.isFaceDown()) cardUnderDragged.flip();
            }
            handleValidMove(card, pile);
        }
        else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }

        if (isGameWon()) {
            AlertBox.popUp("You won", "CONGRATULATIONS, YOU WON!");

        }

    };

    public boolean isGameWon() {
        //TODO
        int sumNumOfCards = 0;

        for (Pile pile : foundationPiles) {
            sumNumOfCards += pile.numOfCards();
        }

        if (sumNumOfCards == 1) {
            return true;
        }
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    /**
     * If th stockPile is empty, and you click on the stockPile its refilled from the discardPile
     */
    public void refillStockFromDiscard() {
        //TODO
        if(stockPile.isEmpty()){
            while (!discardPile.isEmpty()){
                discardPile.getTopCard().flip();
                discardPile.getTopCard().moveToPile(stockPile);
            }
        }
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        //TODO


        if(foundationPiles.contains(destPile)) {
            return isFoundationMoveValid(card, destPile);
        } else if(tableauPiles.contains(destPile)) {
            return isTableauMoveValid(card, destPile);
        }
        return false;
    }


    /**
     * Returns a boolean value depending on if the card can be moved to the foundation field
     *
     * @param card the card dragged by the mouse
     * @param destPile the pile where the card is dropped to
     * @return boolean value depending on if the card can be moved to the foundation field
     */
    public boolean isFoundationMoveValid(Card card, Pile destPile){
        Card topCard = destPile.getTopCard();
        boolean noCardDestPile = (topCard == null);

        if (noCardDestPile) return Rank.ACE.equals(card.getRank());
        else {
            boolean isSequential = topCard.getSuit().equals(card.getSuit())
                                   && topCard.getRank().getNextRank().equals(card.getRank());

            return isSequential;
        }
    };


    public boolean isTableauMoveValid(Card card, Pile destPile){

        Card topCard = destPile.getTopCard();
        Rank topCardRank = (topCard == null) ? null : topCard.getRank();

        Rank[] ranks = Rank.values();

        Rank nextRank = null;
        boolean isSequential = false;

        if (topCard != null) {
            for (int i = 0; i < ranks.length; i++) {
                if (topCardRank.equals(ranks[i]) && ( ranks[i] != Rank.ACE)) {
                    nextRank = ranks[i - 1];
                }
            }
        }

        boolean isFirstCardKing = Rank.KING.equals(card.getRank()) && topCard == null;
        if (nextRank != null){
            isSequential = nextRank.equals(card.getRank()) && Card.isOppositeColor(card, topCard);
        }

        if (isFirstCardKing || isSequential) {
            return true;
        }
        else {
            return false;
        }
    };


    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }


    private Pile getDestPile(Card card, List<Pile> foundationPiles){
        for(Pile pile : foundationPiles){
            if(isFoundationMoveValid(card, pile))
                return pile;
        }
        return card.getContainingPile();
    };


    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
            allPiles.add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
            allPiles.add(tableauPile);
        }
    }

    public void dealCards() {

        Iterator<Card> deckIterator = deck.iterator();
        //TODO

        deckIterator.forEachRemaining(card -> {
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
        setCardsToTableau();
    }

    /**
     * Sets the cards to the tableaus at the init / cards dealing.
     * The fisrt tableau has one card and the seventh tableau has seven card.
     */
    public void setCardsToTableau() {
        for(int i=0; i < tableauPiles.size(); i++){
            for(int j=i; j < tableauPiles.size(); j++) {
                stockPile.getTopCard().moveToPile(tableauPiles.get(j));
            }
            tableauPiles.get(i).getTopCard().flip();
        }
    }


    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

}
