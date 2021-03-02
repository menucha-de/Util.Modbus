#ifndef COMMON_MUTEX_H
#define COMMON_MUTEX_H

#include <pthread.h> // pthread_mutex_t

namespace CommonNamespace {

    class MutexPrivate;

    class Mutex {
    public:
        Mutex()/* throws MutexException */;
        virtual ~Mutex();

        virtual pthread_mutex_t& getMutex();
    private:
        MutexPrivate* d;
    };

} // CommonNamespace
#endif /* COMMON_MUTEX_H */
