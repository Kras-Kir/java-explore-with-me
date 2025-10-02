package ru.practicum.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {


    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto newCategoryDto);

    CategoryDto toCategoryDto(Category category);
}
