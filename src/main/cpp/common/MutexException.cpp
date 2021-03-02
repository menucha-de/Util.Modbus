#include "MutexException.h"

namespace CommonNamespace {

    MutexException::MutexException(const std::string& message,
            const std::string& file, int line) :
    Exception(message, file, line) {
    }

    Exception* MutexException::copy() const {
        return new MutexException(*this);
    }

} // CommonNamespace
