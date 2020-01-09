/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package put.ai.games.theBestPlayer;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.moves.RotateMove;

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

        List<MyMove> moves = board.getMovesFor(getColor()).stream().map(MyMove::new).collect(Collectors.toList());

        Move longestRowExtensionMove = findLongestRowExtension(board, winningLength - 1, moves);
        if (longestRowExtensionMove != null)
        {
            System.out.println(String.format("Trying to extend the longest existing row \t| Time: %dms \t| Available time: %dms", System.currentTimeMillis() - startTime, getTime()));
            return longestRowExtensionMove;
        }

        Optional<MyMove> move = moves.stream()
                .filter(m -> m.getPlaceX() == boardSize / 2)
                .findFirst();
        if (move.isPresent())
        {
            System.out.println(
                    String.format("Trying edge of board quarter \t| Time: %dms \t| Available time: %dms",
                            System.currentTimeMillis() - startTime, getTime()));
            return move.get().getMove();
        }

        move = moves.stream()
                .filter(m -> m.getPlaceY() == boardSize / 2)
                .findFirst();
        if (move.isPresent())
        {
            System.out.println(
                    String.format("Trying edge of board quarter \t| Time: %dms \t| Available time: %dms",
                            System.currentTimeMillis() - startTime, getTime()));
            return move.get().getMove();
        }

        System.out.println(
                String.format("Random turn \t| Time: %dms \t| Available time: %dms",
                        System.currentTimeMillis() - startTime, getTime()));
        return moves.get(random.nextInt(moves.size())).getMove();
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

        System.out.println(patterns);

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
        LEFT_DOWN(1, 1),
        DOWN(0, 1),
        RIGHT_DOWN(-1, 1),
        RIGHT(-1, 0),
        RIGHT_UP(-1, -1),
        UP(0, -1),
        LEFT_UP(1, -1);

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
        private final RotateMove.Direction direction;
        private final Move move;

        MyMove(Move move)
        {
            this.move = move;

            String[] elements = move.toString()
                    .replace("Place@(", "")
                    .replace("), rotate@(", ", ")
                    .replace(")->(", ", ")
                    .replace(") ", ", ").split(", ");

            placeX = Integer.valueOf(elements[0]);
            placeY = Integer.valueOf(elements[1]);

            direction = elements[6]
                    .equalsIgnoreCase("CLOCKWISE") ? RotateMove.Direction.CLOCKWISE : RotateMove.Direction.COUNTERCLOCKWISE;
        }

        int getPlaceX()
        {
            return placeX;
        }

        int getPlaceY()
        {
            return placeY;
        }

        public RotateMove.Direction getDirection()
        {
            return direction;
        }

        Move getMove()
        {
            return move;
        }
    }
}
