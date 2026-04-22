package ru.yandex.practicum.mymarket.dto;

public class Paging {
    public int pageSize;
    public int pageNumber;
    public boolean hasPrevious;
    public boolean hasNext;

    public Paging(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.hasPrevious = hasPrevious;
        this.hasNext = hasNext;
    }

    public int pageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int pageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
