public class ParseError {

    public final int lineNumber;
    public final String message;

    @java.beans.ConstructorProperties({"lineNumber"})
    public ParseError(final int lineNumber) {
        this(lineNumber, "Not valid JSON syntax.");
    }

    public ParseError(final int lineNumber, final String message) {
        this.lineNumber = lineNumber;
        this.message = message;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getMessage() {
        return this.message;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ParseError)) return false;
        final ParseError other = (ParseError) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getLineNumber() != other.getLineNumber()) return false;
        final Object this$message = this.getMessage();
        final Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getLineNumber();
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        return result;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ParseError;
    }

    public String toString() {
        return "ParseError(lineNumber=" + this.getLineNumber() + ", message=" + this.getMessage() + ")";
    }
}
