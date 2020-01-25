/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.theBestPlayer;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.pentago.impl.PentagoBoard;

import java.util.*;
import java.util.stream.Collectors;

public class TheBestPlayer extends Player
{

    private Random random = new Random(0xdeadbeef);

    @Override
    public String getName()
    {
        return "Błażej Krzyżanek 136749 Wojciech Rak 136789";
    }

    @Override
    public Move nextMove(Board board)
    {
        long startTime = System.currentTimeMillis();
        int boardSize = board.getSize();
        int winningLength = boardSize / 2 + (boardSize / 2 + 1) / 2;

        List<MyMove> moves = board.getMovesFor(getColor()).stream().map(o -> new MyMove(o, boardSize)).collect(Collectors.toList());
        MyBoard myBoard = new MyBoard((PentagoBoard) board);
        MyMove bestMove = moves.get(0);
        float bestMoveValue = 0;

        Color myColor = getColor();
        Color opponentColor = Arrays.stream(Color.values()).filter(color -> !(color.equals(myColor) || color.equals(Color.EMPTY))).findAny().orElse(Color.EMPTY);

        for (MyMove myMove : moves)
        {
            float moveValue = calculateMoveValue(myBoard, myMove, winningLength, myColor, opponentColor);
            System.out.println(moveValue);

            if (moveValue > 0.99)
            {
                return myMove.getMove();
            }
            else if (moveValue > bestMoveValue)
            {
                bestMoveValue = moveValue;
                bestMove = myMove;
            }
        }

        List<Color> pattern = new ArrayList<>();
        pattern.add(Color.EMPTY);
        for (int i = 0; i < winningLength - 1; i++)
        {
            pattern.add(opponentColor);
        }

        BoardPosition boardPosition = findPatternOnBoard(board, pattern);
        if (boardPosition != null)
        {
            MyMove blockingMove = moves.stream()
                    .filter(move ->
                            move.placeX == boardPosition.getX()
                                    && move.placeY == boardPosition.getY())
                    .findFirst()
                    .orElse(null);

            if (blockingMove != null && bestMoveValue < 0.99)
            {
                return blockingMove.getMove();
            }
        }

        if(bestMoveValue < 0.2)
        {
            return moves.get(random.nextInt(moves.size())).getMove();
        }

        return bestMove.getMove();
    }

    private float calculateMoveValue(MyBoard board, MyMove move, int winningLength, Color myColor, Color opponentColor)
    {
        Board boardAfterMove = board.getBoardAfterMove(move);

        if (boardAfterMove != null)
        {
            // 2. Jeżeli ruch powoduje wygraną przeciwnika - return 0;
            if (isRowAfterMove(boardAfterMove, opponentColor, winningLength))
            {
                return 0;
            }

            return longestRowOnBoard(boardAfterMove, winningLength) / (float) winningLength;
        }

        return 0.5f;
    }

    private boolean isRowAfterMove(Board boardAfterMove, Color playerColor, int winningLength)
    {
        List<Color> pattern = new ArrayList<>();
        for (int i = 0; i < winningLength; i++)
        {
            pattern.add(playerColor);
        }

        return findPatternOnBoard(boardAfterMove, pattern) != null;
    }


    private int longestRowOnBoard(Board board, int winningLength)
    {
        for (int a = winningLength; a > 1; a--)
        {
            List<Color> pattern = new ArrayList<>();
            for (int i = 0; i < a; i++)
            {
                pattern.add(getColor());
            }

            if (findPatternOnBoard(board, pattern) != null)
            {
                return a;
            }
        }

        return 0;
    }

    private Move findLongestRowExtension(Board board, int winningLength, List<MyMove> moves)
    {
        List<List<Color>> patterns = new ArrayList<>();

        for (int a = 1; a < 3; a++)
        {
            for (int i = 0; i < winningLength - a; i++)
            {
                patterns.add(new ArrayList<>());
                for (int j = 0; j < winningLength - a; j++)
                {
                    if (j == i)
                    {
                        patterns.get(a + i - 1).add(Color.EMPTY);
                    }
                    else
                    {
                        patterns.get(a + i - 1).add(getColor());
                    }
                }
            }
        }

        Optional<BoardPosition> bestPosition = patterns
                .stream()
                .filter(list -> !list.isEmpty())
                .map(p -> findPatternOnBoard(board, p))
                .filter(Objects::nonNull)
                .findAny();

        return bestPosition
                .map(boardPosition -> moves.stream()
                        .filter(m -> m.getPlaceX() == boardPosition.getX())
                        .filter(m -> m.getPlaceY() == boardPosition.getY())
                        .findFirst()
                        .map(MyMove::getMove)
                        .orElse(null))
                .orElse(null);

    }

    private BoardPosition findPatternOnBoard(Board board, List<Color> pattern)
    {
        for (int x = 0; x < board.getSize(); x++)
        {
            for (int y = 0; y < board.getSize(); y++)
            {
                for (PatternDirectionsEnum direction : PatternDirectionsEnum.values())
                {
                    if (doesPatternExist(board, direction, x, y, pattern))
                    {
                        return new BoardPosition(x, y);
                    }
                }
            }
        }

        return null;
    }

    private boolean doesPatternExist(Board board, PatternDirectionsEnum direction, int x, int y, List<Color> pattern)
    {
        if (!board.getState(x, y).equals(pattern.get(0)))
        {
            return false;
        }

        int length = pattern.size();
        int boardSize = board.getSize();

        for (int i = 1; i < length; i++)
        {
            x += direction.getHorizontal();
            if ((x >= boardSize || x < 0) && !pattern.get(i).equals(Color.EMPTY))
            {
                return false;
            }

            y += direction.getVertical();
            if ((y >= boardSize || y < 0) && !pattern.get(i).equals(Color.EMPTY))
            {
                return false;
            }

            if (!board.getState(x, y).equals(pattern.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    private enum PatternDirectionsEnum
    {
        LEFT(1, 0),
//                LEFT_DOWN(1, 1),
        DOWN(0, 1),
//                RIGHT_DOWN(-1, 1),
        RIGHT(-1, 0),
//                RIGHT_UP(-1, -1),
        UP(0, -1);
//        LEFT_UP(1, -1);

        private int horizontal;
        private int vertical;

        PatternDirectionsEnum(int horizontal, int vertical)
        {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }

        public int getHorizontal()
        {
            return horizontal;
        }

        public int getVertical()
        {
            return vertical;
        }
    }

    private enum MyDirection
    {
        LEFT_UPPER_LEFT("lu"),
        LEFT_UPPER_RIGHT("lu"),
        LEFT_LOWER_LEFT("ll"),
        LEFT_LOWER_RIGHT("ll"),
        RIGHT_UPPER_LEFT("ru"),
        RIGHT_UPPER_RIGHT("ru"),
        RIGHT_LOWER_LEFT("rl"),
        RIGHT_LOWER_RIGHT("rl");

        private String quarter;

        MyDirection(String quarter)
        {
            this.quarter = quarter;
        }

        public String getQuarter()
        {
            return quarter;
        }
    }

    private class BoardPosition
    {
        private final int x;
        private final int y;

        public BoardPosition(int x, int y)
        {
            this.x = x;
            this.y = y;
        }

        public int getX()
        {
            return x;
        }

        public int getY()
        {
            return y;
        }
    }

    private class MyMove
    {
        private final int placeX;
        private final int placeY;
        private final MyDirection direction;
        private final Move move;
        private final String placeQuarter;

        MyMove(Move move, int boardSize)
        {
            this.move = move;

            String[] elements = move.toString()
                    .replace("Place@(", "")
                    .replace("), rotate@(", ", ")
                    .replace(")->(", ", ")
                    .replace(") ", ", ").split(", ");

            placeX = Integer.valueOf(elements[0]);
            placeY = Integer.valueOf(elements[1]);

            int middle = boardSize / 2;

            if (placeX < middle)
            {
                if (placeY < middle)
                {
                    placeQuarter = "lu";
                }
                else
                {
                    placeQuarter = "ll";
                }
            }
            else
            {
                if (placeY < middle)
                {
                    placeQuarter = "ru";
                }
                else
                {
                    placeQuarter = "rl";
                }
            }

            int fromX = Integer.valueOf(elements[2]);
            int fromY = Integer.valueOf(elements[3]);
            int toX = Integer.valueOf(elements[4]);
            int toY = Integer.valueOf(elements[5]);

            if (elements[6].equalsIgnoreCase("CLOCKWISE"))
            {
                if (fromX < middle)
                {
                    if (fromY < middle)
                    {
                        direction = MyDirection.LEFT_UPPER_LEFT;
                    }
                    else
                    {
                        direction = MyDirection.LEFT_LOWER_LEFT;
                    }
                }
                else
                {
                    if (fromY < middle)
                    {
                        direction = MyDirection.RIGHT_UPPER_LEFT;
                    }
                    else
                    {
                        direction = MyDirection.RIGHT_LOWER_LEFT;
                    }
                }
            }
            else
            {
                if (fromX < middle)
                {
                    if (fromY < middle)
                    {
                        direction = MyDirection.LEFT_UPPER_RIGHT;
                    }
                    else
                    {
                        direction = MyDirection.LEFT_LOWER_RIGHT;
                    }
                }
                else
                {
                    if (fromY < middle)
                    {
                        direction = MyDirection.RIGHT_UPPER_RIGHT;
                    }
                    else
                    {
                        direction = MyDirection.RIGHT_LOWER_RIGHT;
                    }
                }
            }
        }

        int getPlaceX()
        {
            return placeX;
        }

        int getPlaceY()
        {
            return placeY;
        }

        public MyDirection getDirection()
        {
            return direction;
        }

        Move getMove()
        {
            return move;
        }

//        public int getNewX()
//        {
//            if (direction.getQuarter().equals(placeQuarter))
//            {
//                if (direction)
//            }
//            else
//            {
//                return placeX;
//            }
//        }
    }

    private class MyBoard
    {
        private PentagoBoard boardBeforeMove;

        MyBoard(PentagoBoard boardBeforeMove)
        {
            this.boardBeforeMove = boardBeforeMove;
        }

        PentagoBoard getBoardAfterMove(MyMove move)
        {
            PentagoBoard board = boardBeforeMove.clone();

            board.doMove(move.getMove());

            return board;
        }

        PentagoBoard getBoardBeforeMove()
        {
            return boardBeforeMove;
        }
    }
}
