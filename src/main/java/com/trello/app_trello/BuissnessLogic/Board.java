package com.trello.app_trello.BuissnessLogic;

import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

@Data
public class Board {
    private String id;
    private String name;
    private String owner;
    private ArrayList<Map<String, Object>> lists;
}
