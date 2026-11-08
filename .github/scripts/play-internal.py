#!/usr/bin/env python3
from play_internal.__main__ import main
from play_internal.policy import Stop

try:
    main()
except Stop as error:
    raise SystemExit(str(error)) from None
