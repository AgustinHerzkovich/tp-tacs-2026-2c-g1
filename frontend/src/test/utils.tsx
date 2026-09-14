import type { ReactNode } from "react";
import { configureStore } from "@reduxjs/toolkit";
import { renderHook, type RenderHookOptions } from "@testing-library/react";
import { Provider } from "react-redux";
import sessionReducer, { type SessionState } from "@/store/session/sessionSlice";

export function createTestStore(preloadedSession: Partial<SessionState> = {}) {
  return configureStore({
    reducer: { session: sessionReducer },
    preloadedState: { session: { user: null, initialized: true, ...preloadedSession } },
  });
}

export function renderHookWithStore<Result, Props>(
  render: (props: Props) => Result,
  options: { preloadedSession?: Partial<SessionState>; initialProps?: Props } = {},
) {
  const store = createTestStore(options.preloadedSession);
  const wrapper = ({ children }: { children: ReactNode }) => <Provider store={store}>{children}</Provider>;
  const renderOptions: RenderHookOptions<Props> = {
    wrapper,
    initialProps: options.initialProps,
  };
  return { ...renderHook(render, renderOptions), store };
}