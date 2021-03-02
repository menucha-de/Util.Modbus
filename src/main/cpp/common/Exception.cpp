#include "Exception.h"
#include <stddef.h> // NULL
#include <stdio.h> // asprintf
#include <malloc.h> // free

namespace CommonNamespace {

    class ExceptionPrivate {
        friend class Exception;
    private:
        std::string message;
        std::string file;
        int line;
        Exception* cause;
        unsigned long* errorCode;
    };

    Exception::Exception(const std::string& message, const std::string& file,
            int line) {
        d = new ExceptionPrivate();
        d->message = message;
        d->file = file;
        d->line = line;
        d->cause = NULL;
        d->errorCode = NULL;
    }

    Exception::Exception(const Exception& ex) {
        operator=(ex);
    }

    Exception& Exception::operator=(const Exception& ex) {
        // avoid self-assignment
        if (this == &ex) {
            return *this;
        }
        d = new ExceptionPrivate();
        d->message = ex.d->message;
        d->file = ex.d->file;
        d->line = ex.d->line;
        d->cause = ex.d->cause == NULL ? NULL : ex.d->cause->copy();
        if (ex.d->errorCode != NULL) {
            d->errorCode = new unsigned long;
            *d->errorCode = *ex.d->errorCode;
        } else {
            d->errorCode = NULL;
        }
        return *this;
    }

    Exception* Exception::copy() const {
        return new Exception(*this);
    }

    Exception::~Exception() throw () {
        delete d->cause;
        delete d->errorCode;
        delete d;
    }

    const std::string& Exception::getMessage() const {
        return d->message;
    }

    void Exception::setCause(const Exception* cause) {
        d->cause = cause->copy();
    }

    const Exception* Exception::getCause() const {
        return d->cause;
    }

    void Exception::setErrorCode(unsigned long *errorCode) {
        if (errorCode != NULL) {
            if (d->errorCode == NULL) {
                d->errorCode = new unsigned long;
            }
            *d->errorCode = *errorCode;
        } else {
            delete d->errorCode;
            d->errorCode = NULL;
        }
    }

    const unsigned long* Exception::getErrorCode() const {
        if (d->errorCode == NULL && d->cause != NULL) {
            return d->cause->getErrorCode();
        }
        return d->errorCode;
    }

    void Exception::getStackTrace(std::string& returnStackTrace) {
        char* s;
        asprintf(&s, "%s\n  at %s:%d", what(), d->file.c_str(), d->line);
        returnStackTrace.append(s);
        free(s);
        if (d->cause != NULL) {
            returnStackTrace.append("\nCaused by: ");
            d->cause->getStackTrace(returnStackTrace);
        }
    }

    const char* Exception::what() const throw () {
        return d->message.c_str();
    }

} // namespace CommonNamespace
