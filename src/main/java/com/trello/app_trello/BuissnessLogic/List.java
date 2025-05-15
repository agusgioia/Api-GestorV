package com.trello.app_trello.BuissnessLogic;

import lombok.Data;

import java.util.ArrayList;


@Data
public class List {
    private String title;
    private ArrayList<Card> cards;
}
