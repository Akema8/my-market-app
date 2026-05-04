package ru.yandex.practicum.mymarket.dto;

public class ChangeQuantityForm {
    private Long id;
    private String search;
    private String sort;
    private Integer pageNumber;
    private Integer pageSize;
    private String action;

    public ChangeQuantityForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}