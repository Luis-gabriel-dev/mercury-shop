package dev.adastratech.mercuryshop.shared.application;

/**
 * Parâmetros de paginação/ordenação independentes de framework, para que o domínio e
 * os casos de uso não dependam de tipos do Spring Data (Pageable/Sort).
 */
public record PageQuery(int page, int size, String sortBy, Direction direction) {

    public enum Direction { ASC, DESC }

    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    public PageQuery {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (direction == null) {
            direction = Direction.ASC;
        }
    }
}
