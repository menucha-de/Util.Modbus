#ifndef COMMON_MUTEXLOCK_H
#define COMMON_MUTEXLOCK_H

#include "Mutex.h"

namespace CommonNamespace {

    class MutexLockPrivate;

    class MutexLock {
    public:
        MutexLock(Mutex& mutex) /* throws MutexException */;
        virtual ~MutexLock();

        virtual void lock() /* throws MutexException */;
        virtual void unlock();
    private:
        MutexLockPrivate* d;
    };

} // CommonNamespace
#endif /* COMMON_MUTEXLOCK_H */
