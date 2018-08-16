Make sure to create `passwd` file prior running compose file. `passwd` file name must match VIRTUAL_HOST variable defined in compose file. You can generate it using `htpasswd` utility from `apache2-utils` package:

```
htpasswd -b /var/nginx/htpasswd/{VIRTUAL_HOST} username password
```
