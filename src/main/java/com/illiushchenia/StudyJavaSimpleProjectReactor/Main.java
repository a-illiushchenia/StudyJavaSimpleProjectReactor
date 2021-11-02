package com.illiushchenia.StudyJavaSimpleProjectReactor;

import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        //Создание пустых продьюсеров
        Mono.empty();
        Flux.empty();
        Mono<Integer> mono = Mono.just(1);
        Flux<Integer> flux = Flux.just(1, 2, 3);

        //Mono и Flux легко преобразуются друг в друга
        Flux<Integer> fluxFromMono = mono.flux();
        Mono<Boolean> monoFromFlux = flux.any(s -> s.equals(1));
        Mono<Integer> integerMono = flux.elementAt(1);

        //Создание продьюсера в виде последовательности элементов от 1 до 5
        Flux.range(1, 5);

        Flux.range(1, 5).subscribe(System.out::println);
        System.out.println("1 ---");
        Flux.fromIterable(Arrays.asList(1, 2, 3)).subscribe(System.out::println);
        System.out.println("2 ---");
        //генератор, который генерирует 10 строк "hello"
        Flux.<String>generate(sink ->{
            sink.next("hello");
        })
                .take(10)
                .subscribe(System.out::println);
        System.out.println("3 ---");
        //выводит 4 элемента "hello" с интервалом в пол секунды.
        // Вывод запускается в отдельном потоке, поэтому внизу поставлено Thread.sleep(4000)
        Flux.<String>generate(sink ->{
                    sink.next("hello");
                })
                .delayElements(Duration.ofMillis(500))
                .take(4)
                .subscribe(System.out::println);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("4 ---");
        //понимаю, что здесь происходит, НО НЕ ПОНИМАЮ КАК
        Flux
                .generate(
                        () -> 2345,
                        (state, sink) -> {
                            if (state > 2366) {
                                sink.complete();
                            } else {
                                sink.next("Step: " + state);
                            }

                            return state + 3;
                        }
                )
                .subscribe(System.out::println);

        System.out.println("5 ---");
        //создаем поток имитирует сообщения из внешней системы
        Flux<Object> telegramProducer = Flux
                .generate(
                        () -> 2345,
                        (state, sink) -> {
                            if (state > 2366) {
                                sink.complete();
                            } else {
                                sink.next("Step: " + state);
                            }

                            return state + 3;
                        }
                );

        //обрабатываем поток, который имитирует сообшения из внешней системы.
        //Здесь обрабатываются приходящие от системы сообщения.
        Flux //есть аналогичный метод push. create - многопоточный, push - однопоточны.
                // sink - объект, который позволяет управлять реактивным потоком
                .create(sink ->
                        telegramProducer.subscribe(new BaseSubscriber<Object>() {
                            @Override
                            protected void hookOnNext(Object value) {
                                sink.next(value);
                            }

                            @Override
                            protected void hookOnComplete() {
                                sink.complete();
                            }
                        })
                )
                .subscribe(System.out::println);
        System.out.println("6 ---");

        //имитация работы с БД - здесь мы сами посылаем запрос к БД (в нашем случае к telegramProducer)
        //и обрабатываем полученный ответ.
        Flux
                .create(sink ->
                        sink.onRequest(r -> {
                            sink.next("DB returns: " + telegramProducer.blockFirst());
                        })

                )
                .subscribe(System.out::println);
        System.out.println("7 ---");

        Flux<String> second = Flux
                .just("World", "coder")
                .repeat();
        //склеивание 2-х потоков
        Flux<String> sumFlux = Flux
                .just("hello", "dru", "java", "Linus", "Asia", "java")
                .zipWith(second, (f, s) -> String.format("%s %s", f, s));

        sumFlux
                .subscribe(System.out::println);
        System.out.println("8 ---");

        //вывод с задержкой
        sumFlux
                .delayElements(Duration.ofMillis(300))
                .subscribe(System.out::println);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("9 ---");

        //вывод с задержкой и ожиданием тайм-аута поступления следующего элемента
        sumFlux
                .delayElements(Duration.ofMillis(1300))
                .timeout(Duration.ofSeconds(1))//будет брошен TimeoutException, т.к. элемент придет через 1300 миллисекунд
                .retry(3)//данная функция будет выполнять 3 попытки ожидания .timeout(...)
                //.onErrorReturn("Too slow")// в случае ошибки будет отдано вместо ожидакмого элемента
                //второй вариант обработки ошибки
                .onErrorResume(throwable -> {
                    //можно обработать исключение throwable или вернуть другую последовательность, например:
                    return Flux.just("one", "two");
                })
                .subscribe(System.out::println);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("10 ---");

        sumFlux = Flux
                .just("hello", "dru", "java", "Linus", "Asia", "java")
                .zipWith(second, (f, s) -> String.format("%s %s", f, s));

        Flux<String> stringFlux = sumFlux
                .delayElements(Duration.ofMillis(1300))
                .timeout(Duration.ofSeconds(1))
                .onErrorResume(throwable ->
                        Flux
                                .interval(Duration.ofMillis(300))
                                .map(String::valueOf)
                )
                .skip(2)
                .take(3);

        stringFlux.subscribe(
                v -> System.out.println(v),//обрабатывает приходящие значения
                e -> System.out.println(e),//обрабатывает ошибку
                () -> System.out.println("finished")//обрабатывает завершение стрима
        );

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("11 ---");
        stringFlux.toIterable();
    }
}
