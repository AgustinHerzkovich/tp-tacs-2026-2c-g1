import { configureStore } from "@reduxjs/toolkit";
import sessionReducer from "@/store/session/sessionSlice";
import { sessionPersistenceMiddleware } from "@/store/session/sessionPersistence";

export function makeStore() {
  return configureStore({
    reducer: {
      session: sessionReducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(sessionPersistenceMiddleware),
  });
}

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore["getState"]>;
export type AppDispatch = AppStore["dispatch"];
