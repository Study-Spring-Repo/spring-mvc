# 예외 처리와 오류 페이지

서블릿은 Exception 발생 서블릿 밖으로 전달되거나 response.sendError() 가 호출 되었을 때 설정된 오류 페이지를 찾는다.

- 예외 상태, 예외 타입에 따른 에러 페이지 등록
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

- 예외 페이지 컨트롤러
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

- 예외 발생과 오류 페이지 요청 흐름

1. WAS(여기까지 전파) <- 필터 <- 서블릿 <- 인터셉터 <- 컨트롤러(예외발생)
2. WAS `/error-page/500` 다시 요청 -> 필터 -> 서블릿 -> 인터셉터 -> 컨트롤러(/error- page/500) -> View

- request에 전달된 오류 정보

```java
public static final String ERROR_EXCEPTION = "javax.servlet.error.exception";
public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
public static final String ERROR_MESSAGE = "javax.servlet.error.message";
public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
public static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";
public static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

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

