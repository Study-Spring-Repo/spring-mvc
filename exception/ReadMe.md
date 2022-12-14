# 예외 처리와 오류 페이지

서블릿은 Exception 발생 서블릿 밖으로 전달되거나 response.sendError() 가 호출 되었을 때 설정된 오류 페이지를 찾는다.

> ### 예외 상태, 예외 타입에 따른 에러 페이지 등록

```java
public class WebServletCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {

        ErrorPage errorPage404 = new ErrorPage(HttpStatus.NOT_FOUND, "/error-page/404");
        ErrorPage errorPage500 = new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error-page/500");

        ErrorPage errorPageEx = new ErrorPage(RuntimeException.class, "/error-page/500");

        factory.addErrorPages(errorPage404, errorPage500, errorPageEx);
    }
}
```

> ### 예외 페이지 컨트롤러

```java

@Controller
public class ErrorPageController {

    @RequestMapping("/error-page/404")
    public String errorPage404(HttpServletRequest request, HttpServletResponse response) {
        log.info("errorPage 404");
        printErrorInfo(request);
        return "error-page/404";
    }

    @RequestMapping("/error-page/500")
    public String errorPage500(HttpServletRequest request, HttpServletResponse response) {
        log.info("errorPage 500");
        printErrorInfo(request);
        return "error-page/500";
    }
}
```

> ### request에 전달된 오류 정보

```java
public static final String ERROR_EXCEPTION="javax.servlet.error.exception";
public static final String ERROR_EXCEPTION_TYPE="javax.servlet.error.exception_type";
public static final String ERROR_MESSAGE="javax.servlet.error.message";
public static final String ERROR_REQUEST_URI="javax.servlet.error.request_uri";
public static final String ERROR_SERVLET_NAME="javax.servlet.error.servlet_name";
public static final String ERROR_STATUS_CODE="javax.servlet.error.status_code";

private void printErrorInfo(HttpServletRequest request){
        log.info("ERROR_EXCEPTION: {}",request.getAttribute(ERROR_EXCEPTION));
        log.info("ERROR_EXCEPTION_TYPE: {}",request.getAttribute(ERROR_EXCEPTION_TYPE));
        log.info("ERROR_MESSAGE: {}",request.getAttribute(ERROR_MESSAGE));
        log.info("ERROR_REQUEST_URI: {}",request.getAttribute(ERROR_REQUEST_URI));
        log.info("ERROR_SERVLET_NAME: {}",request.getAttribute(ERROR_SERVLET_NAME));
        log.info("ERROR_STATUS_CODE: {}",request.getAttribute(ERROR_STATUS_CODE));
        log.info("dispatcherType: {}",request.getDispatcherType());
        }
```

> ### 예외 발생과 오류 페이지 요청 흐름

1. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
2. WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error- page/500) -> View

- 오류가 발생하면 오류 페이지를 출력하기 위해 WAS 내부를 다시 호출
  - 필터, 서블릿, 인터셉터를 다시 호출한다.
  - 필터, 인터셉터를 중복 체크하는 경우가 발생한다.
    - 비효율적이다.
    - 이를 해결하기 위해 클라이언트로부터 발생한 요청을 구별해야 한다.
  - `DispatcherType` 정보를 이용한다.

> ### DispatcherType

```java
 public enum DispatcherType {
      FORWARD,
      INCLUDE,
      REQUEST,
      ASYNC,
      ERROR
}
```

- REQUEST : 클라이언트 요청
- ERROR : 오류 요청
- FORWARD : MVC에서 배웠던 서블릿에서 다른 서블릿이나 JSP 호출
- INCLUDE : 서블리세서 다른 서블릿이나 JSP의 결과를 포함
- ASYNC : 서블릿 비동기 호출

DispatcherType 설정
- default : REQUEST
- REQUEST, ERROR  : 클라이언트 요청, 오류 페이지 요청 필터 호출


```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean logFilter() {
        FilterRegistrationBean<Filter> filterFilterRegistrationBean = new FilterRegistrationBean<>();
        filterFilterRegistrationBean.setFilter(new LogFilter());
        filterFilterRegistrationBean.setOrder(1);
        filterFilterRegistrationBean.addUrlPatterns("/*");
        filterFilterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        return filterFilterRegistrationBean;
    }
}
```

> ### Interceptor

- 요청 경로에 따라서 추가하거나 제외할 수 있다.
- `excludePathPatterns`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new LogInterceptor())
            .order(1)
            .addPathPatterns("/**")

            // 오류 페이지 경로
            .excludePathPatterns("/css/**", "*.ico", "/error", "/error-page/**");

  }
}
```

- 전체 흐름 정리

```
// 오류 요청
1. WAS(/error-ex, dispatchType=REQUEST) -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러
2. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
3. WAS 오류 페이지 확인
4. WAS(/error-page/500, dispatchType=ERROR) -> 필터(x) -> 서블릿 -> 인터셉터(x) -> 컨트롤러(/error-page/500) -> View
```

> ### 정적 오류 페이지1

- 스프링부트는 `ErrorPage`를 자동 등록한다.
  - `/error` 경로 : 기본 오류 페이지 설정
  - 서블릿 예외, `response.sendError(...)` 호출시


- 오류 페이지
  - BasicErrorController는 기본적인 로직이 모두 구현되어 있다.
  - 오류 페이지 화면을 BasicErrorController가 제공하는 룰과 우선순위에 따라서 등록한다.


- 뷰 선택 우선순위
  - 뷰 템플릿
    - resources/templates/error/500.html 
    - resources/templates/error/5xx.html
  - 정적 리소스
    - resources/static/error/400.html
    - resources/static/error/4xx.html
  - 적용 대상 없을 때 뷰 이름 (error)
    - resources/templates/error.html


---

# API 예외 처리

> ### BasicErrorController

- 스프링 부트가 제공하는 기본 오류 방식


```java
package org.springframework.boot.autoconfigure.web.servlet.error;

/**
 * Basic global error @Controller, rendering ErrorAttributes. More specific errors can be handled either using 
 * Spring MVC abstractions (e.g. @ExceptionHandler) or by adding servlet server error pages.
 * Since:
 * 1.0.0
 * See Also:
 * ErrorAttributes, ErrorProperties
 * Author:
 * Dave Syer, Phillip Webb, Michael Stummvoll, Stephane Nicoll, Scott Frederick
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class BasicErrorController extends AbstractErrorController {
  // ...

  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
  public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        // ...
  }

  @RequestMapping
  public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        // ...
  }
}
```

- `errorHtml()`
  - produces = MediaType.TEXT_HTML_VALUE
  - 클라이언트 요청의 Accept 헤더 값이 `text/html` 일경우에는 `errorHtml()`을 호출해서 view를 제공한다.
- `error()`
  - 그외 경우에 호출되고 `ResponseEntity`로 HTTP Body에 JSON 데이터를 반환한다.

> ### HandlerExceptionResolver

- 스프링 MVC는 컨트롤러(Handler) 박으로 예외가 던져진 경우 예외를 해결한다.
- 컨트롤러 밖으로 던져진 혜외 해결 후 동작 방식을 변경할 수 있다.

![img.png](img.png)

```java
package org.springframework.web.servlet;

public interface HandlerExceptionResolver {
  ModelAndView resolveException(
          HttpServletRequest request, 
          HttpServletResponse response,
          Object handler, 
          Exception ex);
}
```

- `handler` : 핸들러(컨트롤러) 정보
- `Exception ex` : 핸들러(컨트롤러)에서 발생한 예외


- ExceptionResolver가 ModelAndView를 반환하는 이유
  - Exception을 처리해서 정상 흐름 처럼 변경한다.
  - Exception을 Resolve(해결)한다.


> ### ExceptionResolver

스프링 부트는 `ExceptionResolver`를 기본으로 제공한다.

- `HandlerExceptionResolverComposite`에 다음 순서로 등록한다.
  - `ExceptionHandlerExceptionResolver`
  - `ResponseStatusExceptionResolver`
  - `DefaultHandlerExceptionResolver`

> ### ExceptionHandlerExceptionResolver

- `@ExceptionHandler`를 처리한다.
- API 예외 처리의 대부분을 해결한다.

> ### ResponseStatusExceptionResolver

- 예외에 따라서 HTTP 상태 코드를 지정해준다.
  - ex) `@ResponseStatus(value = HttpStatus.NOT_FOUND)`


- 두 가지 경우를 처리한다.
  - `@ResponseStatus` 예외
  - `ResponseStatusException` 예외

<br>

`@ResponseStatus`
- 애노테이션을 적용하면 HTTP 상태 코드를 변경한다.

```java
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "error.bad")
public class BadRequestException extends RuntimeException {
}
```

- 메시지 기능을 추가한다. 
```properties
# messages.properties
error.bad=잘못된 요청입니다.
```

- `@ResponseStatus` 동작 방식
  - BadRequestException 예외가 컨트롤러 밖으로 넘어간다.
  - ResponseStatusExceptionResolver 예외가 해당 애노테이션을 확인한다.
    - 오류 코드를 설정값으로 변경한다.
    - 메시지를 담는다.
    - `response.sendError(statusCode, resolvedReason)`을 호출한다.

```json
 {
      "status": 400,
      "error": "Bad Request",
      "exception": "hello.exception.exception.BadRequestException", 
      "message": "잘못된 요청입니다.",
      "path": "/api/response-status-ex1"
}
```

<br>

`ResponseStatusException`
- 위에서 이용한 @ResponseStatus는 개발자가 직접 변경할 수 없는 예외에 적용할 수 없다.
- 이럴 경우에 `ResponseStatusException` 예외를 이용한다.


```java
@GetMapping("/api/response-status-ex2")
public String responseStatusEx2() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
      "error.bad", 
      new IllegalArgumentException());
}
```

```json
{
      "status": 404,
      "error": "Not Found",
      "exception": "org.springframework.web.server.ResponseStatusException", 
      "message": "잘못된 요청입니다.",
      "path": "/api/response-status-ex2"
}
```

> ### DefaultHandlerExceptionResolver

- `DefaultHandlerExceptionResolver`
  - 스프링 내부에서 발생하는 스프링 예외를 처리한다.
  - ex) `TypeMismatchException`
    - 파라미터 바인딩 시 예외 발생 처리
    - 원래대로라면 서블릿 컨테이너까지 오류가 올라가고, 결과적으로 `500 오류`를 발생한다.
      - 하지만 `500 오류`보다 `400 오류` 를 발생하는 것이 더 정확하다고 할 수 있다.
      - 파라미터 바인딩 오류는 대부분 클라이언트 측의 잘못된 요청이기 때문이다.
    - `DefaultHandlerExceptionResolver`는 `400 오류`로 변경해준다.

<br>

`DefaultHandlerExceptionResolver` 내부에 `handleTypeMismatch()` 메서드를 보면 `response.sendError()` 메서드를 볼 수 있다.
```java
package org.springframework.web.servlet.mvc.support;

public class DefaultHandlerExceptionResolver extends AbstractHandlerExceptionResolver {
  // ...

  protected ModelAndView handleTypeMismatch(TypeMismatchException ex,
                                            HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws IOException {

    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    return new ModelAndView();
  }
  
  //...
}
```

![img_1.png](img_1.png)