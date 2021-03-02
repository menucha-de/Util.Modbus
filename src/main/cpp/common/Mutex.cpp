#include "Mutex.h"
#include "MutexException.h"

namespace CommonNamespace {

    class MutexPrivate {
        friend class Mutex;
    private:
        pthread_mutex_t mutex;
    };

    Mutex::Mutex() /* throws MutexException */ {
        d = new MutexPrivate();
        // initialize the mutex as reentrant lock
        pthread_mutexattr_t attr;
        int status = pthread_mutexattr_init(&attr);
        if (status == 0) {
            status = pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
            if (status == 0) {
                status = pthread_mutex_init(&d->mutex, &attr);
            }
            int status2 = pthread_mutexattr_destroy(&attr);
            if (status == 0) {
                status = status2;
            }
        }
        if (status != 0) {
            throw ExceptionDef(MutexException, "Cannot initialize mutex");
        }
    }

    Mutex::~Mutex() {
        pthread_mutex_destroy(&d->mutex);
        delete d;
    }

    pthread_mutex_t& Mutex::getMutex() {
        return d->mutex;
    }

} // CommonNamespace
