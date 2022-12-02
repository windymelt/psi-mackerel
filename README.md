## `psi-mackerel`

Tool for posting [Page Speed Insights](https://pagespeed.web.dev/) score to [Mackerel](https://mackerel.io/)

### Usage

PSI Score is in a range of `[0, 100]`.

#### Page Speed Insights score

You can retrieve PSI score:

```shell
$ psi-mackerel https://example.com/
```

You can provide API key for google (see `psi-mackerel --help`):

```shell
$ psi-mackerel --psi-api-key "..." https://example.com/
```

#### Post to Mackerel

You can post score to Mackerel instead of Stdout:

```shell
$ psi-mackerel --mackerel-api-key "..." --service "service_name" https://example.com/
```

Both `--mackerel-api-key` and `--service` are needed.

#### Pass URIs via file

You can give URI list instead of arguments:

```shell
$ psi-mackerel --target-list list.txt
```

URI list should consist of UTF-8 encoded lines like this:

```
https://example.com/1
https://example.com/2
https://example.com/3
```

#### Shorthands

You can view short option via `psi-mackerel --help`.

### Inspired from aereal/psi-metrics

This tool is inspired from https://github.com/aereal/psi-metrics .
