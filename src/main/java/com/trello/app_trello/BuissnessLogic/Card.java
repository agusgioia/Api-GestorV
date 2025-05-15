package com.trello.app_trello.BuissnessLogic;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Card {
    private String title;
    private String description;
    private ArrayList<String> assignedUsers;
}
