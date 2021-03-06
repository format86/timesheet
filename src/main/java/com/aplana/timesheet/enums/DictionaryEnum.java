package com.aplana.timesheet.enums;

/**
 * @author eshangareev
 * @version 1.0
 */
public enum DictionaryEnum implements TSEnum {
    CATEGORY_OF_ACTIVITY(1, "Категория активности"),
    TYPES_OF_ACTIVITY(2, "Тип активности"),
    WORKPLACE(5, "Место работы"),
    OVERTIME_CAUSE(10, "Причины переработок"),
    UNFINISHED_DAY_CAUSE(11, "Причины недоработок");


    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private DictionaryEnum(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
