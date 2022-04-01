## SpringBoot Validation

### BindingResult
ModelAttribute 바로 뒤에 BindingResult를 파라미터로 받을 수 있다.  
BindingResult는 요청 데이터를 ModelAttribute 객체에 바인딩하는 과정에서 발생하는 오류를 담고 있는 객체.  
BindingResult가 없다면 ModelAttribute 객체 바인딩이 실패 시 컨트롤러가 호출되지 않는다.
BindingResult를 ModelAttribute 뒤에서 받으면 컨트롤러가 호출되고 바인딩 과정에서 발생한 문제를 저장하고 다시 클라이언트에게 보내줄 수 있다.

```java
public String addGoods(Goods goods, BindingResult bindingResult) {
    //...
    return "goods";
}
```
BindingResult는 addError 메서드를 가지는 인터페이스이다.  
Field 하나에 관련된 예외를 처리하는 FieldError, 그 외 복합 필드예외를 처리하는 ObjectError.
```java
public interface BindingResult extends Errors {

    /**
     * Add a custom {@link ObjectError} or {@link FieldError} to the errors list.
     * <p>Intended to be used by cooperating strategies such as {@link BindingErrorProcessor}.
     * @see ObjectError
     * @see FieldError
     * @see BindingErrorProcessor
     */
    void addError(ObjectError error);
    
}

```
FieldError 가 ObjectError를 상속한다.
```java
public class FieldError extends ObjectError {

    private final String field;

    @Nullable
    private final Object rejectedValue;
    
    // ...
    /**
     * Create a new FieldError instance.
     * @param objectName the name of the affected object
     * @param field the affected field of the object
     * @param defaultMessage the default message to be used to resolve this message
     */
    public FieldError(String objectName, String field, String defaultMessage) {
        this(objectName, field, null, false, null, null, defaultMessage);
    }

    /**
     * Create a new FieldError instance.
     * @param objectName the name of the affected object
     * @param field the affected field of the object
     * @param rejectedValue the rejected field value
     * @param bindingFailure whether this error represents a binding failure
     * (like a type mismatch); else, it is a validation failure
     * @param codes the codes to be used to resolve this message
     * @param arguments the array of arguments to be used to resolve this message
     * @param defaultMessage the default message to be used to resolve this message
     */
    public FieldError(String objectName, String field, @Nullable Object rejectedValue, boolean bindingFailure,
        @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

        super(objectName, codes, arguments, defaultMessage);
        Assert.notNull(field, "Field must not be null");
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.bindingFailure = bindingFailure;
    }
```

### Bean Validation
spring-boot-starter-validation 라이브러리가 자동으로 Bean Validator를 context에 등록한다.  
LocalValidatorFactoryBean. 이 Validator는 모델에 붙은 @NotNull @NotBlank 등의 어노테이션으로 검증을 수행.  
검증 오류가 발생하면 FieldError, ObjectError 롤 BindingResult에 등록한다.  

### @Validated or @Valid    
@Validated 는 스프링, @Valid 는 javax  
```java
public String addGoods(@Validated Goods goods) {
//...
return "goods";
}
```

### @ModelAttribute VS @RequestBody
ModelAttribute 는 필드 단위로 바인딩을 적용된다
- 특정 필드가 바인딩되지 않아도 검증이 수행된다.  

RequestBody (HttpMessageConverter) 는 Json 데이터를 객체로 변경하지 못 하면 예외가 발생한다.
- 이후 단계인 검증이 수행되지 않는다. 
- 발생하는 예외는 MethodArgumentNotValidException.  
- 스프링은 MethodArgumentNotValidException 발생 시 400 status (BAD_REQUEST) 처리  

**RestController**를 **@ControllerAdvice**로 처리

```java
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
    
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, 
        HttpHeaders headers, 
        HttpStatus status,
        WebRequest request) {
    
        FieldErrorResponse fieldErrorResponse = new FieldErrorResponse();
        
        List<CustomFieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
                CustomFieldError fieldError = new CustomFieldError();
                fieldError.setField(((FieldError) error).getField());
                fieldError.setMessage(error.getDefaultMessage());
                fieldErrors.add(fieldError);
        });
        
        fieldErrorResponse.setFieldErrors(fieldErrors);
    
        return new ResponseEntity<>(fieldErrorResponse, status);        
    }
    
}
```

### ResponseEntityExceptionHandler
Spring MVC 예외를 처리하고 ResponseEntity를 리턴하는 추상클래스. @ExceptionHandler 메서드를 제공한다.  
Rest API @ControllerAdvice 클래스가 이 클래스를 구현.  

```java
public abstract class ResponseEntityExceptionHandler {

	/**
	 * Provides handling for standard Spring MVC exceptions.
	 * @param ex the target exception
	 * @param request the current request
	 */
	@ExceptionHandler({
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MissingServletRequestParameterException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodArgumentNotValidException.class,
			MissingServletRequestPartException.class,
			BindException.class,
			NoHandlerFoundException.class,
			AsyncRequestTimeoutException.class
		})
	@Nullable
	public final ResponseEntity<Object> handleException(Exception ex, WebRequest request) throws Exception {
		HttpHeaders headers = new HttpHeaders();

		if (ex instanceof HttpRequestMethodNotSupportedException) {
			HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
			return handleHttpRequestMethodNotSupported((HttpRequestMethodNotSupportedException) ex, headers, status, request);
		}
        // ...
		else if (ex instanceof MethodArgumentNotValidException) {
			HttpStatus status = HttpStatus.BAD_REQUEST;
			return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, request);
		}
		// ...
		else {
			// Unknown exception, typically a wrapper with a common MVC exception as cause
			// (since @ExceptionHandler type declarations also match first-level causes):
			// We only deal with top-level MVC exceptions here, so let's rethrow the given
			// exception for further processing through the HandlerExceptionResolver chain.
			throw ex;
		}
	}

	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {

		return handleExceptionInternal(ex, null, headers, status, request);
	}

	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}
		return new ResponseEntity<>(body, headers, status);
	}

}

```

### Test
```java
@WebMvcTest
public class UserControllerTests {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void 이메일주소누락_createUser_400() throws Exception {
        User user = new User(1L, "", "123456");
        String content = asJsonString(user);
        mockMvc.perform(post("/user")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Email is required."))
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }

    @Test
    public void 이메일포맷맞지않음_createUser_400() throws Exception {
        User user = new User(1L, "jdfad", "123456");
        String content = asJsonString(user);
        mockMvc.perform(post("/user")
                .content(content)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("Email is not well formmatted."))
            .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentNotValidException));
    }
    
    private String asJsonString(Object obj) throws Exception {
        return new ObjectMapper().writeValueAsString(obj);
    }
}
```