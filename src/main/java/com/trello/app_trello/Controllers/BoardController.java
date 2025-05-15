package com.trello.app_trello.Controllers;

import com.trello.app_trello.BuissnessLogic.Board;
import com.trello.app_trello.BuissnessLogic.Card;
import com.trello.app_trello.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = {"https://gestordeproyectosytareas.netlify.app","http://localhost:3000"})
public class BoardController {

    @Autowired
    private BoardService boardService;

    //BOARDS
    // Obtener todos los tableros
    @GetMapping
    public ResponseEntity<List<Board>> getAllBoards() {
        List<Board> boards = boardService.getAllBoards();
        return ResponseEntity.ok(boards);
    } 

    // Obtener un tablero por ID
    @GetMapping("/{id}")
    public ResponseEntity<Board> getBoardById(@PathVariable String id) {
        return boardService.getBoardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Obtener tableros por usuario
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Board>> getBoardsByUser(@PathVariable String userId) {
        List<Board> boards = boardService.getBoardsByUser(userId);
        return ResponseEntity.ok(boards != null? boards: Collections.emptyList());
    }

    // Crear tablero
    @PostMapping
    public ResponseEntity<Board> createBoard(@RequestBody Board board) {
        Board created = boardService.createBoard(board);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Actualizar tablero
    @PutMapping("/{id}")
    public ResponseEntity<Board> updateBoard(@PathVariable String id, @RequestBody Board board) {
        if (!id.equals(board.getId())) {
            return ResponseEntity.badRequest().build();
        }
        Board updated = boardService.updateBoard(id,board);
        return ResponseEntity.ok(updated);
    }

    // Eliminar tablero
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable String id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    //LISTS
    //Crear list
    @PostMapping("/{boardId}/lists")
    public ResponseEntity<com.trello.app_trello.BuissnessLogic.List> addListToBoard(
            @PathVariable String boardId,
            @RequestBody com.trello.app_trello.BuissnessLogic.List list) {

        if (list.getTitle() == null || list.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            com.trello.app_trello.BuissnessLogic.List createdList = boardService.AddList(boardId, list);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    //Editar lista

    //Eliminar lista
    @DeleteMapping("/{boardId}/lists/{listTitle}")
    public ResponseEntity<Void> deleteList(@PathVariable String boardId, @PathVariable String listTitle){
        boardService.deleteList(boardId,listTitle);
        return ResponseEntity.noContent().build();
    }

    //CARDS
    //Crear card
    @PostMapping("/{boardId}/lists/{listTitle}/cards")
    public ResponseEntity<Card> addCardToList(
            @PathVariable String boardId,
            @PathVariable String listTitle,
            @RequestBody Card card) {

        // Validaciones básicas
        if (card.getTitle() == null || card.getTitle().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Card createdCard = boardService.AddCard(boardId, listTitle, card);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al procesar la solicitud",
                    e
            );
        }
    }

    //Eliminar card
    @DeleteMapping("/{boardId}/lists/{listTitle}/cards/{cardTitle}")
    public ResponseEntity<Void> deleteCard (@PathVariable String boardId, @PathVariable String listTitle,@PathVariable String cardTitle){
        boardService.deleteCard(boardId,listTitle,cardTitle);
        return ResponseEntity.noContent().build();
    }

}