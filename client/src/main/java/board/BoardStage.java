package board;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import client.Client;
import game.Game;
import game.board.field.Field;
import game.board.piece.Piece;
import game.player.Player;
import gui.SkipTurnEvent;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import run.Run;
/**
 * A classe <code>BoardStage</code> representa o palco principal onde o jogo ocorre.
 * <p>
 * Este palco contém a representação gráfica do tabuleiro de jogo, botões para interação com o jogo,
 * e lida com eventos de interface do usuário como cliques e fechamentos de janela.
 * </p>
 *
 * @version 1.0
 * @since 2024-06-25
 */

public class BoardStage extends Stage implements EventHandler<MouseEvent> {
	@FXML
	private Game game;
	private Player player;
	private PieceCircle activePiece;
	private Client client;
	private boolean active;
	private List<PieceCircle> pieces;
	private Label turnLabel;
	private Button skipButton;
	private Label colorLabel;
	private SkipTurnEvent skipEvent;

	public BoardStage(Game game, int numberOfPlayer, Client client) {
		this.game = game;
		this.setResizable(false);

		this.activePiece = null;
		this.player = game.getPlayerByNumber(numberOfPlayer);
		this.colorLabel = new Label("És o " + player.getColor().toString() + ".");
		this.client = client;
		this.active = false;
		this.pieces = new ArrayList<PieceCircle>();
		this.turnLabel = new Label("Espera pela tua vez...");
		this.skipButton = new Button("Saltar a vez");
		this.skipEvent = new SkipTurnEvent(client, this);
		skipButton.setOnAction(skipEvent);

		drawBoard();

		this.setOnCloseRequest(event -> {
			event.consume();
			handleCloseRequest();
		});
	}
	/**
	 * Método para lidar com o fecho da janela.
	 */
	private void handleCloseRequest() {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Exit Game");
		alert.setHeaderText("Deseja sair do jogo?");
		alert.setContentText("Escolha uma opção.");

		ButtonType buttonTypeSim = new ButtonType("Sim");
		ButtonType buttonTypeNao = new ButtonType("Não");

		alert.getButtonTypes().setAll(buttonTypeSim, buttonTypeNao);

		alert.showAndWait().ifPresent(type -> {
			if (type == buttonTypeSim) {
				// Disconnect the client and close the stage
				client.disconnect();
				this.close();
				Platform.exit();
			} else if (type == buttonTypeNao) {
				alert.close();
			}
		});
	}
	/**
	 * Método para desenhar o tabuleiro de jogo na interface gráfica.
	 */
	private void drawBoard() {
		Group group = new Group();

		for (Field field : this.game.getBoard().getFields()) {
			this.drawField(field, group);
		}

		for (Piece piece : this.game.getPieces()) {
			this.drawPiece(piece, group);
		}

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.add(colorLabel, 0, 0);
		grid.add(turnLabel, 1, 0);
		grid.add(skipButton, 2, 0);

		Button backButton = new Button("Voltar");
		backButton.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Confirm Exit");
				alert.setHeaderText(null);
				alert.setContentText("Deseja sair do jogo?");

				ButtonType buttonTypeYes = new ButtonType("Sim");
				ButtonType buttonTypeNo = new ButtonType("Não");

				alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == buttonTypeYes){
					client.disconnect();
					close();
                    try {
                        new Run().start(new Stage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
					alert.close();
				}
			}
		});

		BorderPane borderPane = new BorderPane();
		borderPane.setTop(backButton);
		BorderPane.setAlignment(backButton, Pos.TOP_LEFT);
		borderPane.setCenter(group);
		borderPane.setBottom(grid);

		Scene scene = new Scene(borderPane, borderPane.prefWidth(0) * 2, BoardData.fieldSize * 2 + borderPane.prefHeight(0) + 100);
		this.setScene(scene);

	}
	/**
	 * Método para desenhar um campo (Field) no tabuleiro de jogo.
	 *
	 *  O objeto a ser desenhado.
	 *  O grupo onde o campo será adicionado.
	 */
	private void drawField(Field field, Group group) {
		FieldCircle fieldCircle = new FieldCircle(field, this);
		group.getChildren().add(fieldCircle);
	}
	/**
	 * Método para desenhar uma peça (Piece) no tabuleiro de jogo.
	 *
	 *  piece A peça a ser desenhada.
	 *   O grupo onde a peça será adicionada.
	 */
	private void drawPiece(Piece piece, Group group) {
		PieceCircle pieceCircle = new PieceCircle(piece, this);
		this.pieces.add(pieceCircle);
		group.getChildren().add(pieceCircle);
	}
	/**
	 * Verifica se o elemento do tabuleiro pertence ao jogador atual.
	 *
	 * O elemento do tabuleiro a ser verificado.
	 *  se o elemento pertencer ao jogador atual, caso contrário código = falso.
	 */
	private boolean isMyElement(BoardElement element) {
		return element.getColor().equals(player.getColor());
	}
	/**
	 * Obtém o objeto PieceCircle associado a uma peça específica.
	 *
	 *  A peça <code>Piece</code> para a qual se deseja obter o objeto PieceCircle.
	 *  O objeto <code>PieceCircle</code> associado à peça especificada.
	 * @throws Exception Se a peça não existir na lista de peças.
	 */
	private PieceCircle getPieceCircle(Piece piece) throws Exception {
		for(PieceCircle pieceCircle: this.pieces) {
			if(pieceCircle.getPiece() == piece)
				return pieceCircle;
		}
		throw new Exception("Piece doesn't exist");
	}
	/**
	 * Define o texto do rótulo de turno.
	 *
	 *  O texto a ser definido no rótulo de turno.
	 */
	public void setLabel(String string) {
		this.turnLabel.setText(string);
	}
	/**
	 * Ativa o tabuleiro, permitindo que o jogador interaja com ele quando é a vez dele.
	 */
	public void activate() {
		this.active = true;
		this.skipEvent.activate();
	}
	/**
	 * Desativa o tabuleiro, impedindo que o jogador interaja com ele quando não é a vez dele.
	 */
	public void setUnactive() {
		this.active = false;
	}

	/**
	 * Realiza um movimento no jogo com base em uma linha de movimento especificada.
	 *
	 *  A linha de movimento a ser processada.
	 * @throws Exception Se houver algum erro ao processar o movimento.
	 */
	public void makeMove(String moveLine) throws Exception {
		String[] line = moveLine.split(" ");
		int initialRow = Integer.parseInt(line[1]);
		int initialDiagonal = Integer.parseInt(line[2]);
		Piece piece = game.getPieceByField(game.getFieldByCoordinates(initialRow, initialDiagonal));
		int destRow = Integer.parseInt(line[3]);
		int destDiagonal = Integer.parseInt(line[4]);
		Field newPosition = game.getFieldByCoordinates(destRow, destDiagonal);
		this.getPieceCircle(piece).move(newPosition, client);

		this.activePiece = null;
		this.active = false;
		this.skipEvent.setUnactive();
	}

	@Override
	public void handle(MouseEvent event) {

		Object source = event.getSource();
		BoardElement element = (BoardElement) source;

		if(this.active == true && element.isPiece() && isMyElement(element)) {
			this.activePiece = (PieceCircle) element;
		}
		else if(element.isField() && this.active == true) {
			FieldCircle fieldCircle = (FieldCircle) element;
			Field newPosition = fieldCircle.getField();
			if(this.activePiece != null) { ;
				client.sendOption(activePiece.getPiece().getPosition().positionToString() + " " + newPosition.positionToString());
			}
		}
	}


	private void goBackToRun() throws IOException {
		Run run = new Run();
		run.start(new Stage());
		this.close();
	}
}
