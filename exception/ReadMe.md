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

> ### 정적 오류 페이지

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


