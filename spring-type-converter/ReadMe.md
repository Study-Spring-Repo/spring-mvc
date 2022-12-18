
### 01. Converter

> 스프링에서 확장 가능한 `Converter` 인터페이스를 제공하고 있다.
- org.springframework.core.convert.converter.Converter

Converter의 내부 구조는 다음과 같다.

```java
package org.springframework.core.convert.converter;

public interface Converter<S, T> {
        T convert(S source);
}
```

Converter 인터페이스를 구현해서 사용하면 된다.

- 문자를 숫자로 변환하는 Converter 구현체

```java
public class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
        log.info("convert source = {}", source);
        return Integer.valueOf(source);
    }
}
```
- 문자를 숫자로 변환하는 Converter 구현체 테스트
```java
@Test
void string_to_integer() {
    StringToIntegerConverter converter = new StringToIntegerConverter();
    Integer result = converter.convert("10");
    assertThat(result).isEqualTo(10);
}
```

### 02. 다양한 방식의 타입 컨버터

> 스프링은 용도에 따라 다양한 타입 컨버터를 제공하고 있다.
> 
> 스프링은 내부에서 ConversionService를 사용해서 타입을 변환하고 있다.

- Converter
  - 기본 타입 컨버터
- ConverterFactory
  - 전체 클래스 계층 구조
- GenericConverter
  - 정교한 구현
  - 대상 필드의 애노테이션 정보 사용 가능
- ConditionalGenericConverter
  - 특정 조건이 참인 경우에만 실행하는 컨버터

### 03. ConversionService

> 스프링은 각각의 컨버터를 모아서 묶어 사용하는 기능을 제공한다.

- ConversionService
  - 컨버터 사용
- ConverterRegistry
  - 컨버터 등록
- DefaultConversionService
  - ConversionService의 구현체
  - ConverterRegistry의 구현체

```java
@Test
void conversion_service() {
    // 등록
    DefaultConversionService conversionService = new DefaultConversionService();
    conversionService.addConverter(new StringToIntegerConverter());
    conversionService.addConverter(new IntegerToStringConverter());
    conversionService.addConverter(new StringToIpPortConverter());
    conversionService.addConverter(new IpPortToStringConverter());

    // 사용
    assertThat(conversionService.convert("10", Integer.class))
            .isEqualTo(10);

    assertThat(conversionService.convert(10, String.class))
            .isEqualTo("10");

    assertThat(conversionService.convert("127.0.0.1:8080", IpPort.class))
            .isEqualTo(new IpPort("127.0.0.1", 8080));

    assertThat(conversionService.convert(new IpPort("127.0.0.1", 8080), String.class))
            .isEqualTo("127.0.0.1:8080");
}
```

