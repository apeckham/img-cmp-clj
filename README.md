# img-cmp-clj

Compares images from "actual" and "expected" directories, and creates an image gallery of the differences between the two, like BBC-News/wraith.

![Example](example.png)

## Developing

```
lein repl
(use 'img-cmp-clj.core :reload) (-main)
open out.html
```