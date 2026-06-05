package com.liud.ainocodegenerator.langgraph4j.node;

import com.liud.ainocodegenerator.langgraph4j.state.ImageCategoryEnum;
import com.liud.ainocodegenerator.langgraph4j.state.ImageResource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDeduplicationNodeTest {

    @Test
    void deduplicate_removesNullUrlResources() {
        List<ImageResource> input = Arrays.asList(
                ImageResource.builder().url(null).category(ImageCategoryEnum.CONTENT).build(),
                ImageResource.builder().url("https://example.com/a.png").category(ImageCategoryEnum.CONTENT).build()
        );

        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("https://example.com/a.png");
    }

    @Test
    void deduplicate_removesBlankUrlResources() {
        List<ImageResource> input = Arrays.asList(
                ImageResource.builder().url("  ").category(ImageCategoryEnum.CONTENT).build(),
                ImageResource.builder().url("https://example.com/b.png").category(ImageCategoryEnum.ILLUSTRATION).build()
        );

        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(input);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUrl()).isEqualTo("https://example.com/b.png");
    }

    @Test
    void deduplicate_removesDuplicateUrls() {
        List<ImageResource> input = Arrays.asList(
                ImageResource.builder().url("https://example.com/img.png").category(ImageCategoryEnum.CONTENT).description("first").build(),
                ImageResource.builder().url("https://example.com/img.png").category(ImageCategoryEnum.LOGO).description("duplicate").build(),
                ImageResource.builder().url("https://example.com/other.png").category(ImageCategoryEnum.ARCHITECTURE).build()
        );

        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("first");
        assertThat(result.get(1).getUrl()).isEqualTo("https://example.com/other.png");
    }

    @Test
    void deduplicate_preservesOrderOfFirstOccurrence() {
        List<ImageResource> input = Arrays.asList(
                ImageResource.builder().url("https://a.com/1.png").build(),
                ImageResource.builder().url("https://b.com/2.png").build(),
                ImageResource.builder().url("https://a.com/1.png").build(),
                ImageResource.builder().url("https://c.com/3.png").build()
        );

        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(input);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ImageResource::getUrl)
                .containsExactly("https://a.com/1.png", "https://b.com/2.png", "https://c.com/3.png");
    }

    @Test
    void deduplicate_handlesEmptyList() {
        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void deduplicate_trimsUrlBeforeComparison() {
        List<ImageResource> input = Arrays.asList(
                ImageResource.builder().url("https://example.com/img.png").build(),
                ImageResource.builder().url("  https://example.com/img.png  ").build()
        );

        List<ImageResource> result = ResourceDeduplicationNode.deduplicate(input);

        assertThat(result).hasSize(1);
    }
}
