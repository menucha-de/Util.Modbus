#include "MutexLock.h"
#include "MutexException.h"

namespace CommonNamespace {

    class MutexLockPrivate {
        friend class MutexLock;
    private:
        Mutex* mutex;
        bool isLocked;
    };

    MutexLock::MutexLock(Mutex& mutex) /* throws MutexException */ {
        d = new MutexLockPrivate();
        d->mutex = &mutex;
        d->isLocked = false;
        lock(); // MutexException
    }

    MutexLock::~MutexLock() {
        unlock();
        delete d;
    }

    void MutexLock::lock() /* throws MutexException */ {
      if (!d->isLocked) {
        int status = pthread_mutex_lock(&d->mutex->getMutex());
        if (status != 0) {
            throw ExceptionDef(MutexException, "Cannot lock mutex");
        }
        d->isLocked = true;
      }
    }

    void MutexLock::unlock() {
      if (d->isLocked) {
        pthread_mutex_unlock(&d->mutex->getMutex());
        d->isLocked = false;
      }
    }

} // CommonNamespace
