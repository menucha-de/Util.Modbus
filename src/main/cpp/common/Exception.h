#ifndef COMMON_EXCEPTION_H
#define COMMON_EXCEPTION_H

#include <exception>
#include <string>

#define ExceptionDef(type, msg) type(msg, __FILE__, __LINE__)

namespace CommonNamespace {

    class ExceptionPrivate;

    class Exception : public std::exception {
    public:
        // Copies of the given message and file path are saved internally.
        Exception(const std::string& message, const std::string& file, int line);
        // Creates a deep copy of the instance.
        Exception(const Exception& ex);
        // Creates a deep copy of the instance.
        Exception& operator=(const Exception& ex);
        // Creates a deep copy of the instance.
        // Derived classes should override this method and return an instance of the derived type.
        // The returned instance must be destroyed by the caller.
        virtual Exception* copy() const;
        virtual ~Exception() throw ();

        virtual const std::string& getMessage() const;
        // A copy of the given exception is saved internally.
        virtual void setCause(const Exception* cause);
        virtual const Exception* getCause() const;
        virtual void setErrorCode(unsigned long* errorCode);
        virtual const unsigned long* getErrorCode() const;
        virtual void getStackTrace(std::string& returnStackTrace);

        // interface std::exception
        virtual const char* what() const throw ();
    private:
        ExceptionPrivate* d;
    };

} // namespace CommonNamespace
#endif /* COMMON_EXCEPTION_H */
