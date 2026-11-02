package br.com.petflow.petflow_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final HttpServletRequest request;

    public GlobalExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "Entidade não encontrada", ex.getMessage(), null, null, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return buildError(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage(), null, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), null, null, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos", null, null, null);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Token inválido", ex.getMessage(), ex.getCode(), null, null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "E-mail ou senha inválidos", null, null, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", ex.getMessage(), ex.getCode(), null, null);
    }

    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPoints(InsufficientPointsException ex) {

        Map<String, Object> details = new HashMap<>();
        details.put("availablePoints", ex.getAvailablePoints());
        details.put("requiredPoints", ex.getRequiredPoints());

        return buildError(HttpStatus.UNPROCESSABLE_CONTENT, "Pontos insuficientes", ex.getMessage(), ex.getCode(), details, null);
    }

    @ExceptionHandler(ExpiredCouponException.class)
    public ResponseEntity<ErrorResponse> handleExpiredCoupon(ExpiredCouponException ex) {

        Map<String, Object> details = new HashMap<>();
        details.put("couponCode", ex.getCouponCode());
        details.put("expirationDate", ex.getExpirationDate());

        return buildError(HttpStatus.UNPROCESSABLE_CONTENT, "Cupom expirado", ex.getMessage(), ex.getCode(), details, null);
    }

    @ExceptionHandler(CouponAlreadyRedeemedException.class)
    public ResponseEntity<ErrorResponse> handleCouponAlreadyRedeemed(CouponAlreadyRedeemedException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_CONTENT, "Cupom já resgatado", ex.getMessage(), ex.getCode(), null, null);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex) {

        Map<String, Object> details = new HashMap<>();
        details.put("entityType", ex.getEntityType());
        details.put("currentStatus", ex.getCurrentStatus());
        details.put("targetStatus", ex.getTargetStatus());

        return buildError(HttpStatus.UNPROCESSABLE_CONTENT, "Transição de status inválida", ex.getMessage(), ex.getCode(), details, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {

                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();

                    errors.put(fieldName, errorMessage);
                });

        return buildError(HttpStatus.BAD_REQUEST, "Erro de validação", "Campos inválidos", null, null, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (error1, error2) -> error1
                ));

        return buildError(HttpStatus.BAD_REQUEST, "Erro de validação", "Violação de restrições", null, null, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        String detail = cause == null ? "" : cause.toLowerCase();
        String message = "Violação de integridade de dados";
        if (detail.contains("ora-00001") || detail.contains("unique") || detail.contains("duplicate")) {
            message = "Registro duplicado. Verifique os campos únicos.";
        } else if (detail.contains("ora-02292") || detail.contains("child record")) {
            message = "O registro possui dependências e não pode ser removido.";
        } else if (detail.contains("ora-02291") || detail.contains("parent key") || detail.contains("foreign key")) {
            message = "Registro relacionado não encontrado.";
        } else if (detail.contains("ora-01400") || detail.contains("cannot be null")) {
            message = "Existem campos obrigatórios não preenchidos.";
        }
        log.warn("Violação de integridade: {}", cause);
        return buildError(HttpStatus.CONFLICT, "Erro de integridade", message, null, null, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String message = String.format(
                "O parâmetro '%s' recebeu um valor inválido: '%s'",
                ex.getName(),
                ex.getValue()
        );

        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro inválido", message, null, null, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro inválido", ex.getMessage(), null, null, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        return buildError(HttpStatus.BAD_REQUEST, "JSON inválido", "O corpo da requisição está mal formatado.", null, null, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex) {

        String message = "O parâmetro '" + ex.getParameterName() + "' é obrigatório.";
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente", message, null, null, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado", "O método HTTP informado não é suportado para esta rota.", null, null, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de mídia não suportado",
                "Use Content-Type: application/json.", null, null, null);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(Exception ex) {
        return buildError(HttpStatus.NOT_FOUND, "Endpoint não encontrado", "A rota solicitada não existe.", null, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", "Ocorreu um erro inesperado.", null, null, null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status,
            String error,
            String message,
            String code,
            Map<String, Object> details,
            Map<String, String> validationErrors) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .code(code)
                .details(details)
                .validationErrors(validationErrors)
                .path(getPath())
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    private String getPath() {
        return request.getRequestURI();
    }
}