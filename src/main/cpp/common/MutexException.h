#ifndef COMMON_MUTEXEXCEPTION_H
#define COMMON_MUTEXEXCEPTION_H

#include "Exception.h"
#include <string>

namespace CommonNamespace {

    class MutexException : public CommonNamespace::Exception {
    public:
        // Copies of the given message and file path are saved internally.
        MutexException(const std::string& message, const std::string& file,
                int line);

        // interface Exception
        virtual CommonNamespace::Exception* copy() const;
    };

} // CommonNamespace
#endif /* COMMON_MUTEXEXCEPTION_H */
