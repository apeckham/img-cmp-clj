# img-cmp-clj

Compares images from "actual" and "expected" directories, and creates an image gallery of the differences between the two, like BBC-News/wraith. Exits 0 if all actual files matched expected files.

![Example](example.png)

## Developing

```
lein repl
(use 'img-cmp-clj.core :reload) (write (compare-all))
open out.html
```

## Todo

- Print what's happening
- Better error when diff directory is missing
- Take input directories as flags
- Take out.html path as a flag