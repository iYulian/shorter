package ru.url.shorter.service;

import ru.url.shorter.common.User;
import ru.url.shorter.exception.UserDuplicateException;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class UriShorterMainMenu {

    public static final String WORK_END = "Ввод завершён. Работа приложения окончена";

    public static void main(String[] args) {
        try (ShortedUriObserver shortedUriObserver = new ShortedUriObserver()) {
            Scanner scanner = new Scanner(System.in);
            User user = null;
            while (true) {
                try {
                    user = UriShorterUserService.getUser(user, scanner);
                } catch (IllegalArgumentException | UserDuplicateException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
                if (user == null) {
                    System.out.println("Нет активного пользователя. Выход из системы");
                    break;
                }
                System.out.println("""
                        Для пользователя %s доступны действия:
                        0 - Закончить работу.
                        1 - Сменить пользователя.
                        2 - Работа со ссылками.
                        """.formatted(user.getLogin()));
                int action = URIShorterUtil.scanLimitedInt(scanner, 0, 2);
                if (action == 0) {
                    break;
                }
                if (action == 1) {
                    user = UriShorterUserService.registerOrLogintUser(user, scanner);
                    continue;
                }
                if (action == 2) {
                    UriShorterUriService.uriWorker(user, scanner);
                    continue;
                }
                break;
            }
        } catch (NoSuchElementException e) {
            System.out.println(WORK_END);
        }
    }
}
