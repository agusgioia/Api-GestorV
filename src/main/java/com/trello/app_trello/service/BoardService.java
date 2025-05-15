package com.trello.app_trello.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.trello.app_trello.BuissnessLogic.Board;
import com.trello.app_trello.BuissnessLogic.Card;
import com.trello.app_trello.Firebase.Firebase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private static final String BOARD_COLLECTION = "boards";
    private static final String OWNER_FIELD = "owner";

    @Autowired
    private Firebase firebase;

    // Obtener todos los tableros
    public List<Board> getAllBoards() {
        try {
            QuerySnapshot querySnapshot = getCollection().get().get();
            return querySnapshot.getDocuments().stream()
                    .map(this::documentToBoard)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener todos los tableros",
                    e
            );
        }
    }

    // Obtener un tablero por ID
    public Optional<Board> getBoardById(String id) {
        if (id == null || id.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de tablero no válido"
            );
        }

        try {
            DocumentSnapshot document = getCollection().document(id).get().get();
            return document.exists()
                    ? Optional.of(documentToBoard(document))
                    : Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener el tablero con ID: " + id,
                    e
            );
        }
    }

    // Obtener tableros por usuario
    public List<Board> getBoardsByUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de usuario no válido"
            );
        }

        try {
            QuerySnapshot querySnapshot = getCollection()
                    .whereEqualTo(OWNER_FIELD, userId)
                    .get()
                    .get();

            return querySnapshot.getDocuments().stream()
                    .map(this::documentToBoard)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al obtener tableros del usuario: " + userId,
                    e
            );
        }
    }

    // Crear un nuevo tablero
    public Board createBoard(Board board) {
        try {
            DocumentReference docRef = getCollection().document();
            board.setId(docRef.getId());

            ApiFuture<WriteResult> writeResult = docRef.set(board);
            writeResult.get();

            return board;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear el tablero",
                    e
            );
        }
    }

    // Actualizar un tablero existente
    public Board updateBoard(String id, Board board) {
        if (!id.equals(board.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID del path no coincide con ID del tablero"
            );
        }

        try {
            ApiFuture<WriteResult> writeResult = getCollection()
                    .document(id)
                    .set(board);

            writeResult.get();
            return board;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al actualizar el tablero",
                    e
            );
        }
    }

    // Eliminar un tablero
    public void deleteBoard(String id) {
        try {
            ApiFuture<WriteResult> deleteResult = getCollection()
                    .document(id)
                    .delete();

            deleteResult.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al eliminar el tablero",
                    e
            );
        }
    }

    public void deleteCard(String boardId, String listTitle, String cardTitle) {
        try {
            DocumentReference docRef = getCollection().document(boardId);
            DocumentSnapshot snapshot = docRef.get().get();

            if (!snapshot.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tablero no encontrado");
            }

            // Convertir a objeto y obtener listas
            Board board = documentToBoard(snapshot);
            List<Map<String, Object>> allLists = new ArrayList<>(board.getLists());

            // 1. Buscar la lista usando streams (más limpio que for+break)
            Optional<Map<String, Object>> targetListOpt = allLists.stream()
                    .filter(list -> listTitle.equals(list.get("title")))
                    .findFirst();

            if (targetListOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lista no encontrada");
            }

            Map<String, Object> targetList = targetListOpt.get();
            List<Map<String, Object>> cards = (List<Map<String, Object>>) targetList.get("cards");

            // 2. Filtrar la card a eliminar (sin usar Iterator)
            List<Map<String, Object>> updatedCards = cards.stream()
                    .filter(card -> !cardTitle.equals(card.get("title")))
                    .toList();

            if (cards.size() == updatedCards.size()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card no encontrada");
            }

            // 3. Actualizar la lista y el documento
            targetList.put("cards", updatedCards);
            docRef.update("lists", allLists).get();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al eliminar la card",
                    e
            );
        }
    }

    public Card AddCard(String boardId, String listTitle, Card card) {
        if (boardId == null || boardId.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de tablero no válido"
            );
        }

        try {
            // 1. Obtener referencia al documento del tablero
            DocumentReference boardRef = getCollection().document(boardId);
            DocumentSnapshot boardSnapshot = boardRef.get().get();

            if (!boardSnapshot.exists()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tablero no encontrado"
                );
            }

            // 2. Convertir a objeto Board
            Board board = documentToBoard(boardSnapshot);

            // 3. Inicializar lists si es null
            if (board.getLists() == null) {
                board.setLists(new ArrayList<>());
            }

            // 4. Buscar o crear la lista
            Optional<Map<String, Object>> listOpt = findOrCreateList(board.getLists(), listTitle);
            Map<String, Object> targetList = listOpt.get();

            // 5. Obtener o inicializar cards
            List<Map<String, Object>> cards = (List<Map<String, Object>>) targetList.get("cards");
            if (cards == null) {
                cards = new ArrayList<>();
                targetList.put("cards", cards);
            }

            // 6. Agregar la nueva card
            cards.add(convertCardToMap(card));

            // 7. Actualizar Firestore
            ApiFuture<WriteResult> updateResult = boardRef.update("lists", board.getLists());
            updateResult.get();

            return card;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al agregar card al tablero",
                    e
            );
        }
    }

    private Optional<Map<String, Object>> findOrCreateList(List<Map<String, Object>> lists, String listTitle) {
        Optional<Map<String, Object>> existingList = lists.stream()
                .filter(list -> listTitle.equals(list.get("title")))
                .findFirst();

        if (existingList.isPresent()) {
            return existingList;
        }

        Map<String, Object> newList = new HashMap<>();
        newList.put("title", listTitle);
        newList.put("cards", new ArrayList<>());
        lists.add(newList);
        return Optional.of(newList);
    }

    private Map<String, Object> convertCardToMap(Card card) {
        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("title", card.getTitle());
        cardMap.put("description", card.getDescription());
        cardMap.put("assignedUsers", card.getAssignedUsers() != null ?
                card.getAssignedUsers() : new ArrayList<>());
        return cardMap;
    }

    //Crear lista
    public com.trello.app_trello.BuissnessLogic.List AddList(String boardId, com.trello.app_trello.BuissnessLogic.List list) {
        if (boardId == null || boardId.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de tablero no válido"
            );
        }

        try {
            // 1. Obtener referencia al documento del tablero
            DocumentReference boardRef = getCollection().document(boardId);
            DocumentSnapshot boardSnapshot = boardRef.get().get();

            if (!boardSnapshot.exists()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tablero no encontrado"
                );
            }

            // 2. Convertir a objeto Board
            Board board = documentToBoard(boardSnapshot);

            // 3. Inicializar lists si es null
            if (board.getLists() == null) {
                board.setLists(new ArrayList<>());
            }

            // 4. Verificar si la lista ya existe
            boolean listExists = board.getLists().stream()
                    .anyMatch(existingList -> list.getTitle().equals(existingList.get("title")));

            if (listExists) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una lista con este título"
                );
            }

            // 5. Crear la nueva lista como Map para Firestore
            Map<String, Object> newListMap = new HashMap<>();
            newListMap.put("title", list.getTitle());
            newListMap.put("cards", new ArrayList<>()); // Inicializar array de cards vacío

            // 6. Agregar la nueva lista al tablero
            List<Map<String, Object>> updatedLists = new ArrayList<>(board.getLists());
            updatedLists.add(newListMap);

            // 7. Actualizar el tablero en Firestore
            ApiFuture<WriteResult> updateResult = boardRef.update("lists", updatedLists);
            updateResult.get();

            // 8. Retornar la lista creada
            com.trello.app_trello.BuissnessLogic.List createdList = new com.trello.app_trello.BuissnessLogic.List();
            createdList.setTitle(list.getTitle());
            createdList.setCards(new ArrayList<>());

            return createdList;

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al agregar lista al tablero",
                    e
            );
        }
    }

    public void deleteList (String id, String title){
        if (id == null || id.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID de tablero no válido"
            );
        }
        try {
            DocumentReference documentReference = getCollection().document(id);
            DocumentSnapshot documentSnapshot = documentReference.get().get();

            Board board = documentToBoard(documentSnapshot);
            List <Map<String, Object>> lists = board.getLists();

            if (lists!=null){
                boolean removed = lists.removeIf(lista ->title.equals(lista.get("title")));
                if(removed){
                    ApiFuture<WriteResult> future = documentReference.update("lists",lists);
                    WriteResult result = future.get();
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al eliminar el tablero",
                    e
            );
        }
    }

    // --- Métodos privados ---
    private CollectionReference getCollection() {
        return firebase.getFirestore().collection(BOARD_COLLECTION);
    }
    private Map<String, Object> cardToMap(Card card) {
        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("title", card.getTitle());
        cardMap.put("description", card.getDescription());
        cardMap.put("assignedUsers", card.getAssignedUsers());
        return cardMap;
    }
    private Board documentToBoard(DocumentSnapshot document) {
        try {
            Board board = new Board();
            board.setId(document.getId());
            board.setName(document.getString("name"));
            board.setOwner(document.getString("owner"));
            board.setLists((ArrayList<Map<String, Object>>) document.get("lists"));
            return board;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al convertir documento a Board",
                    e
            );
        }
    }
}