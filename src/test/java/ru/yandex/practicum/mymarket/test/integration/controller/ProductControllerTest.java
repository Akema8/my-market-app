package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.controller.ProductController;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    public void testGetProducts() throws Exception {
        Page<ProductDto> mockPage = new PageImpl<>(Arrays.asList(
                new ProductDto(1L, "Product1", "Desc1", "img1.png", 100L, 2),
                new ProductDto(2L, "Product2", "Desc2", "img2.png", 200L, 0)
        ));

        when(productService.findItems(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockPage);

        mockMvc.perform(get("/items")
                .param("search", "")
                .param("sort", "NO")
                .param("pageNumber", "1"))
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", "NO"));
    }

    @Test
    public void testGetProducts_WithParams() throws Exception {
        Page<ProductDto> mockPage = new PageImpl<>(Arrays.asList(
                new ProductDto(1L, "Product1", "Desc1", "img1.png", 100L, 2),
                new ProductDto(2L, "Product2", "Desc2", "img2.png", 200L, 0),
                new ProductDto(3L, "Product3", "Desc3", "img3.png", 300L, 1)
        ));
        when(productService.findItems(eq("searchTerm"), eq("PRICE"), eq(2), eq(3)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/items")
                        .param("search", "searchTerm")
                        .param("sort", "PRICE")
                        .param("pageNumber", "2")
                        .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("items"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("search"))
                .andExpect(model().attributeExists("sort"))
                .andExpect(model().attributeExists("paging"))
                .andExpect(model().attribute("search", "searchTerm"))
                .andExpect(model().attribute("sort", "PRICE"));
    }

    @Test
    public void testGetItemPage() throws Exception {
        ProductDto mockProduct = new ProductDto();
        mockProduct.setId(1L);
        mockProduct.setTitle("Test Product");

        when(productService.getItemById(1L)).thenReturn(mockProduct);

        mockMvc.perform(get("/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", mockProduct));
    }
}