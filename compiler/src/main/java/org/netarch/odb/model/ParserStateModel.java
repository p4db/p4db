package org.netarch.odb.model;

import com.google.common.collect.Lists;

import java.util.List;

public class ParserStateModel {
    private final String name;
    private final int id;
    private HeaderModel header;
    private List<String> nextStates;

    /**
     * Create a parser model.s
     *
     * @param name name of the parser state
     * @param id   id of the parser state
     */
    ParserStateModel(String name, int id) {
        this.name = name;
        this.id = id;
        this.header = null;
        this.nextStates = Lists.newArrayList();
    }

    /**
     * Get name of the parser.
     *
     * @return parser name
     */
    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public HeaderModel getHeader() {
        return header;
    }

    public void setHeader(HeaderModel header) {
        this.header = header;
    }

    public List<String> getNextStates() {
        return nextStates;
    }

    public void addNextState(String nextState) {
        this.nextStates.add(nextState);
    }

    /**
     * Is the parser at the end of the packet parsing FSM.
     *
     * @return true if it's the last parser, otherwise false
     */
    public boolean isEnd() {
        return nextStates.isEmpty();
    }
}
